package org.opencb.opencga.analysis.files;

import org.opencb.datastore.core.ObjectMap;
import org.opencb.datastore.core.QueryOptions;
import org.opencb.datastore.core.QueryResult;
import org.opencb.opencga.catalog.db.api.CatalogFileDBAdaptor;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.io.CatalogIOManager;
import org.opencb.opencga.catalog.utils.CatalogFileUtils;
import org.opencb.opencga.catalog.CatalogManager;
import org.opencb.opencga.catalog.models.File;
import org.opencb.opencga.catalog.models.Study;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
public class FileScanner {

    private static Logger logger = LoggerFactory.getLogger(FileScanner.class);

    protected final CatalogManager catalogManager;

    private CatalogFileUtils catalogFileUtils;

    public enum FileScannerPolicy {
        DELETE,     //Delete file and file entry. Then create a new one
        REPLACE,    //Delete the file, but not the file entry. Updates the file information.
//        DO_ERROR,
//        RENAME,
    }

    public FileScanner(CatalogManager catalogManager) {
        this.catalogManager = catalogManager;
        catalogFileUtils = new CatalogFileUtils(catalogManager);
    }

    /**
     * Check tracking from all files from a study.
     * Set file status File.Status.MISSING if the file (fileUri) is unreachable
     * Set file status to File.Status.READY if was File.Status.MISSING and file (fileUri) is reachable
     * @param study         The study to ckeck
     * @param sessionId     User sessionId
     * @throws CatalogException
     * @return found and lost files
     */
    public List<File> checkStudyFiles(Study study, boolean calculateChecksum, String sessionId) throws CatalogException {
        QueryOptions queryOptions = new QueryOptions();
        queryOptions.put(CatalogFileDBAdaptor.FileFilterOption.status.toString(), Arrays.asList(
                File.Status.READY, File.Status.MISSING, File.Status.TRASHED));
        QueryResult<File> files = catalogManager.getAllFiles(study.getId(),
                queryOptions,
                sessionId);

        List<File> modifiedFiles = new LinkedList<>();
        for (File file : files.getResult()) {
            File checkedFile = catalogFileUtils.checkFile(file, calculateChecksum, sessionId);
            if (checkedFile != file) {
                modifiedFiles.add(checkedFile);
            }
        }
        return modifiedFiles;
    }

    /**
     * Scan the study folder, add all untracked files and check tracking
     *
     * @param study                 Study to resync
     * @param calculateChecksum     Calculate Checksum of files
     * @return                      New, lost and found files
     * @throws CatalogException
     * @throws IOException
     */
    public List<File> reSync(Study study, boolean calculateChecksum, String sessionId)
            throws CatalogException, IOException {
        int studyId = study.getId();
//        File root = catalogManager.searchFile(studyId, new QueryOptions("path", ""), sessionId).first();
        QueryOptions query = new QueryOptions();
        query.put(CatalogFileDBAdaptor.FileFilterOption.uri.toString(), "~.*"); //Where URI exists
        query.put(CatalogFileDBAdaptor.FileFilterOption.type.toString(), File.Type.FOLDER);
        List<File> files = catalogManager.searchFile(studyId, query, sessionId).getResult();

        List<File> scan = new LinkedList<>();
        for (File file : files) {
            scan.addAll(scan(file, catalogManager.getFileUri(file), FileScannerPolicy.REPLACE, calculateChecksum,
                    false, sessionId));
            scan.addAll(checkStudyFiles(study, calculateChecksum, sessionId));
        }

        return scan;
    }

    /**
     * Return all untracked files in a study folder
     * @param study         Study to scan
     * @return              Untracked files
     * @throws CatalogException
     */
    public Map<String, URI> untrackedFiles(Study study, String sessionId)
            throws CatalogException {
        int studyId = study.getId();
        URI studyUri = catalogManager.getStudyUri(studyId);

        CatalogIOManager ioManager = catalogManager.getCatalogIOManagerFactory().get(studyUri);
        Map<String, URI> linkedFolders = new HashMap<>();
        linkedFolders.put("", studyUri);
        QueryOptions query = new QueryOptions("include", "projects.studies.files.path,projects.studies.files.uri");
        query.put(CatalogFileDBAdaptor.FileFilterOption.uri.toString(), "~.*"); //Where URI exists
        catalogManager.getAllFiles(studyId, query, sessionId).getResult().forEach(f -> linkedFolders.put(f.getPath(), f.getUri()));

        Map<String, URI> untrackedFiles = new HashMap<>();
        for (Map.Entry<String, URI> entry : linkedFolders.entrySet()) {
            if (!ioManager.exists(entry.getValue())) {
                untrackedFiles.put(entry.getKey(), entry.getValue());
                continue;
            }
            List<URI> files = ioManager.listFiles(entry.getValue());

            for (URI uri : files) {
                String filePath = entry.getKey() + entry.getValue().relativize(uri).toString();

                QueryResult<File> searchFile = catalogManager.searchFile(studyId,
                        new QueryOptions("path", filePath),
                        new QueryOptions("include", "projects.studies.files.id"), sessionId);
                if (searchFile.getResult().isEmpty()) {
                    untrackedFiles.put(filePath, uri);
                } /*else {
                    iterator.remove(); //Remove the ones that have an entry in Catalog
                }*/
            }
        }
        return untrackedFiles ;
    }

    /**
     * Scans the files inside the specified URI and adds to the provided directory.
     *
     * @param directory             Directory where add found files
     * @param directoryToScan       Directory to scan
     * @throws CatalogException
     * @return found and new files.
     */
    public List<File> scan(File directory, URI directoryToScan, FileScannerPolicy policy,
                           boolean calculateChecksum, boolean deleteSource, String sessionId)
            throws IOException, CatalogException {
        return scan(directory, directoryToScan, policy, calculateChecksum, deleteSource, -1, sessionId);
    }

    /**
     * Scans the files inside the specified URI and adds to the provided directory.
     *
     * @param directory             Directory where add found files
     * @param directoryToScan       Directory to scan
     * @param jobId                 If any, the job that has generated this files
     * @throws CatalogException
     * @return found and new files.
     */
    public List<File> scan(File directory, URI directoryToScan, FileScannerPolicy policy,
                           boolean calculateChecksum, boolean deleteSource, int jobId, String sessionId)
            throws IOException, CatalogException {
        if (directoryToScan == null) {
            directoryToScan = catalogManager.getFileUri(directory);
        }
        if (!directoryToScan.getPath().endsWith("/")) {
            directoryToScan = URI.create(directoryToScan.toString() + "/");
        }
        if (!directory.getType().equals(File.Type.FOLDER)) {
            throw new CatalogException("Expected folder where place the found files.");
        }
        int studyId = catalogManager.getStudyIdByFileId(directory.getId());

        List<URI> uris = catalogManager.getCatalogIOManagerFactory().get(directoryToScan).listFiles(directoryToScan);

        List<File> files = new LinkedList<>();
        for (URI uri : uris) {
            URI generatedFile = directoryToScan.relativize(uri);
            String filePath = Paths.get(directory.getPath(), generatedFile.toString()).toString();

            QueryResult<File> searchFile = catalogManager.searchFile(studyId, new QueryOptions("path", filePath), sessionId);

            File file = null;
            boolean returnFile = false;
            if (searchFile.getNumResults() != 0) {
                File existingFile = searchFile.first();
                logger.info("File already existing in target \"" + filePath + "\". FileScannerPolicy = " + policy);
                switch (policy) {
                    case DELETE:
                        logger.info("Deleting file { id:" + existingFile.getId() + ", path:\"" + existingFile.getPath() + "\" }");
                        catalogManager.deleteFile(existingFile.getId(), sessionId);
                        break;
                    case REPLACE:
                        file = existingFile;
                        break;
//                    case RENAME:
//                        throw new UnsupportedOperationException("Unimplemented policy 'rename'");
//                    case DO_ERROR:
//                        throw new UnsupportedOperationException("Unimplemented policy 'error'");
                }
            }

            if (file == null) {
                file = catalogManager.createFile(studyId, FormatDetector.detect(uri), BioformatDetector.detect(uri), filePath, "", true, jobId, sessionId).first();
                logger.info("Added new file " + uri + " { id:" + file.getId() + ", path:\"" + file.getPath() + "\" }");
                /** Moves the file to the read output **/
                catalogFileUtils.upload(uri, file, null, sessionId, false, false, deleteSource, calculateChecksum);
                returnFile = true;      //Return file because is new
            } else {
                if (file.getStatus().equals(File.Status.MISSING)) {
                    logger.info("File { id:" + file.getId() + ", path:\"" + file.getPath() + "\" } recover tracking from file " + uri);
                    logger.info("Set status to " + File.Status.READY);
                    returnFile = true;      //Return file because was missing
                }
                catalogFileUtils.upload(uri, file, null, sessionId, true, true, deleteSource, calculateChecksum);
            }

            try {
                FileMetadataReader.get(catalogManager).setMetadataInformation(file, null, null, sessionId, false);
            } catch (Exception e) {
                logger.error("Unable to read metadata information from file { id:" + file.getId() + ", name: \"" + file.getName() + "\" }", e);
            }

            if (returnFile) { //Return only new and found files.
                files.add(catalogManager.getFile(file.getId(), sessionId).first());
            }
        }
        return files;
    }

}
