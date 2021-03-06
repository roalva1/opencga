/*
 * Copyright 2015 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.opencga.server.ws;

import com.wordnik.swagger.annotations.*;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opencb.biodata.models.feature.Region;
import org.opencb.datastore.core.ObjectMap;
import org.opencb.datastore.core.QueryOptions;
import org.opencb.datastore.core.QueryResponse;
import org.opencb.datastore.core.QueryResult;
import org.opencb.opencga.analysis.AnalysisExecutionException;
import org.opencb.opencga.analysis.storage.AnalysisFileIndexer;
import org.opencb.opencga.catalog.db.api.CatalogFileDBAdaptor;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.models.DataStore;
import org.opencb.opencga.catalog.models.File;
import org.opencb.opencga.catalog.models.Index;
import org.opencb.opencga.catalog.models.Job;
import org.opencb.opencga.catalog.exceptions.CatalogIOException;
import org.opencb.opencga.core.common.Config;
import org.opencb.opencga.core.common.IOUtils;
import org.opencb.opencga.core.exception.VersionException;
import org.opencb.opencga.storage.core.StorageManagerException;
import org.opencb.opencga.storage.core.StorageManagerFactory;
import org.opencb.opencga.storage.core.alignment.AlignmentStorageManager;
import org.opencb.opencga.storage.core.alignment.adaptors.AlignmentDBAdaptor;
import org.opencb.opencga.storage.core.variant.VariantStorageManager;
import org.opencb.opencga.storage.core.variant.adaptors.VariantDBAdaptor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.*;


@Path("/{version}/files")
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Files", position = 4, description = "Methods for working with 'files' endpoint")
public class FileWSServer extends OpenCGAWSServer {


    public FileWSServer(@PathParam("version") String version, @Context UriInfo uriInfo,
                        @Context HttpServletRequest httpServletRequest)
            throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException, VersionException {
        super(version, uriInfo, httpServletRequest);
//        String alignmentManagerName = properties.getProperty("STORAGE.ALIGNMENT-MANAGER", MONGODB_ALIGNMENT_MANAGER);
//        String alignmentManagerName = MONGODB_ALIGNMENT_MANAGER;
//        String variantManagerName = MONGODB_VARIANT_MANAGER;

//        if (variantStorageManager == null) {
//            variantStorageManager = (VariantStorageManager) Class.forName(variantManagerName).newInstance();
//        }
//        if(alignmentStorageManager == null) {
//            alignmentStorageManager = (AlignmentStorageManager) Class.forName(alignmentManagerName).newInstance();
////            try {
////                alignmentStorageManager = (AlignmentStorageManager) Class.forName(alignmentManagerName).newInstance();
////            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
////                e.printStackTrace();
////                logger.error(e.getMessage(), e);
////            }
//            //dbAdaptor = alignmentStorageManager.getDBAdaptor(null);
//        }
    }

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create file with POST method", position = 1, response = QueryResult.class, notes =
            "This method only creates the file entry in Catalog.<br>" +
                    "Will accept (but not yet): acl.<br>" +
                    "<ul>" +
                    "<il><b>id</b> parameter will be ignored.<br></il>" +
                    "<il><b>type</b> accepted values: [<b>'FOLDER', 'FILE', 'INDEX'</b>].<br></il>" +
                    "<il><b>format</b> accepted values: [<b>'PLAIN', 'GZIP', 'EXECUTABLE', 'IMAGE'</b>].<br></il>" +
                    "<il><b>bioformat</b> accepted values: [<b>'VARIANT', 'ALIGNMENT', 'SEQUENCE', 'NONE'</b>].<br></il>" +
                    "<il><b>status</b> accepted values (admin required): [<b>'INDEXING', 'STAGE', 'UPLOADED', 'READY', 'TRASHED', 'DELETED'</b>].<br></il>" +
                    "<il><b>creatorId</b> should be the same as que sessionId user (unless you are admin) </il>" +
                    "<ul>")
    public Response createFilePOST(@ApiParam(value = "studyId", required = true) @QueryParam("studyId") String studyIdStr,
                                   @ApiParam(value = "files", required = true) List<File> files) {
//        List<File> catalogFiles = new LinkedList<>();
        List<QueryResult<File>> queryResults = new LinkedList<>();
        int studyId;
        try {
            studyId = catalogManager.getStudyId(studyIdStr);
        } catch (CatalogException e) {
            e.printStackTrace();
            return createErrorResponse(e);
        }
        for (File file : files) {
            try {
                QueryResult<File> fileQueryResult = catalogManager.createFile(studyId, file.getType(), file.getFormat(),
                        file.getBioformat(), file.getPath(), file.getOwnerId(), file.getCreationDate(),
                        file.getDescription(), file.getStatus(), file.getDiskUsage(), file.getExperimentId(),
                        file.getSampleIds(), file.getJobId(), file.getStats(), file.getAttributes(), true, queryOptions, sessionId);
//                file = fileQueryResult.getResult().get(0);
                System.out.println("fileQueryResult = " + fileQueryResult);
                queryResults.add(fileQueryResult);
            } catch (Exception e) {
                queryResults.add(new QueryResult<>("createFile", 0, 0, 0, "", e.getMessage(), Collections.<File>emptyList()));
//            return createErrorResponse(e.getMessage());
            }
        }
        return createOkResponse(queryResults);
    }

    @GET
    @Path("/create-folder")
    @ApiOperation(value = "Create folder", position = 2)
    public Response createFolder(@ApiParam(value = "studyId", required = true) @QueryParam("studyId") String studyIdStr,
                                 @ApiParam(value = "folder", required = true) @QueryParam("folder") String folder) {
        try {
            java.nio.file.Path folderPath = Paths.get(folder);
            boolean parents = true;
            int studyId = catalogManager.getStudyId(studyIdStr);
            QueryResult queryResult = catalogManager.createFolder(studyId, folderPath, parents, queryOptions, sessionId);
            return createOkResponse(queryResult);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{fileId}/info")
    @ApiOperation(value = "File info", position = 3)
    public Response info(@PathParam(value = "fileId") @DefaultValue("") @FormDataParam("fileId") String fileId) {
        try {
            String[] fieldIdArray = fileId.split(",");
            List<QueryResult> results = new LinkedList<>();
            for (String id : fieldIdArray) {
                results.add(catalogManager.getFile(catalogManager.getFileId(id), this.queryOptions, sessionId));
            }
            return createOkResponse(results);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @ApiOperation(httpMethod = "POST", position = 4, value = "Resource to upload a file by chunks", response = QueryResponse.class)
    public Response chunkUpload(@FormDataParam("chunk_content") byte[] chunkBytes,
                                @FormDataParam("chunk_content") FormDataContentDisposition contentDisposition,
                                @DefaultValue("") @FormDataParam("chunk_id") String chunk_id,
                                @DefaultValue("false") @FormDataParam("last_chunk") String last_chunk,
                                @DefaultValue("") @FormDataParam("chunk_total") String chunk_total,
                                @DefaultValue("") @FormDataParam("chunk_size") String chunk_size,
                                @DefaultValue("") @FormDataParam("chunk_hash") String chunkHash,
                                @DefaultValue("false") @FormDataParam("resume_upload") String resume_upload,

                                @ApiParam(value = "filename", required = true) @DefaultValue("") @FormDataParam("filename") String filename,
                                @ApiParam(value = "fileFormat", required = true) @DefaultValue("") @FormDataParam("fileFormat") String fileFormat,
                                @ApiParam(value = "bioFormat", required = true) @DefaultValue("") @FormDataParam("bioFormat") String bioFormat,
                                @ApiParam(value = "userId", required = true) @DefaultValue("") @FormDataParam("userId") String userId,
//                                @ApiParam(value = "projectId", required = true) @DefaultValue("") @FormDataParam("projectId") String projectId,
                                @ApiParam(value = "studyId", required = true) @FormDataParam("studyId") String studyIdStr,
                                @ApiParam(value = "relativeFilePath", required = true) @DefaultValue("") @FormDataParam("relativeFilePath") String relativeFilePath,
                                @ApiParam(value = "description", required = true) @DefaultValue("") @FormDataParam("description") String description,
                                @ApiParam(value = "parents", required = true) @DefaultValue("true") @FormDataParam("parents") boolean parents) {

        long t = System.currentTimeMillis();

        java.nio.file.Path filePath = null;
        final int studyId;
        try {
            studyId = catalogManager.getStudyId(studyIdStr);
        } catch (Exception e) {
            return createErrorResponse(e);
        }

        try {
            filePath = Paths.get(catalogManager.getFileUri(studyId, relativeFilePath));
            System.out.println(filePath);
        } catch (CatalogIOException e) {
            System.out.println("catalogManager.getFilePath");
            e.printStackTrace();
        } catch (CatalogException e) {
            e.printStackTrace();
        }

        java.nio.file.Path completedFilePath = filePath.getParent().resolve("_" + filename);
        java.nio.file.Path folderPath = filePath.getParent().resolve("__" + filename);

        logger.info(relativeFilePath + "");
        logger.info(folderPath + "");
        logger.info(filePath + "");
        boolean resume = Boolean.parseBoolean(resume_upload);

        try {
            logger.info("---resume is: " + resume);
            if (resume) {
                logger.info("Resume ms :" + (System.currentTimeMillis() - t));
                return createOkResponse(getResumeFileJSON(folderPath));
            }

            int chunkId = Integer.parseInt(chunk_id);
            int chunkSize = Integer.parseInt(chunk_size);
            boolean lastChunk = Boolean.parseBoolean(last_chunk);

            logger.info("---saving chunk: " + chunkId);
            logger.info("lastChunk: " + lastChunk);

            // WRITE CHUNK TYPE_FILE
            if (!Files.exists(folderPath)) {
                logger.info("createDirectory(): " + folderPath);
                Files.createDirectory(folderPath);
            }
            logger.info("check dir " + Files.exists(folderPath));
            // String hash = StringUtils.sha1(new String(chunkBytes));
            // logger.info("bytesHash: " + hash);
            // logger.info("chunkHash: " + chunkHash);
            // hash = chunkHash;
            if (chunkBytes.length == chunkSize) {
                Files.write(folderPath.resolve(chunkId + "_" + chunkBytes.length + "_partial"), chunkBytes);
            } else {
                String errorMessage = "Chunk content size (" + chunkBytes.length + ") " +
                        "!= chunk_size (" + chunk_size + ").";
                logger.error(errorMessage);
                return createErrorResponse(new IOException(errorMessage));
            }

            if (lastChunk) {
                logger.info("lastChunk is true...");
                Files.deleteIfExists(completedFilePath);
                Files.createFile(completedFilePath);
                List<java.nio.file.Path> chunks = getSortedChunkList(folderPath);
                logger.info("----ordered chunks length: " + chunks.size());
                for (java.nio.file.Path partPath : chunks) {
                    logger.info(partPath.getFileName().toString());
                    Files.write(completedFilePath, Files.readAllBytes(partPath), StandardOpenOption.APPEND);
                }
                IOUtils.deleteDirectory(folderPath);
                try {
                    QueryResult queryResult = catalogManager.createFile(studyId, File.Format.valueOf(fileFormat.toUpperCase()),
                            File.Bioformat.valueOf(bioFormat.toUpperCase()), relativeFilePath, completedFilePath.toUri(),
                            description, parents, sessionId
                    );
                    return createOkResponse(queryResult);
                } catch (Exception e) {
                    logger.error(e.toString());
                    return createErrorResponse(e);
                }
            }
        } catch (IOException e) {
            System.out.println("e = " + e);
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        logger.info("chunk saved ms :" + (System.currentTimeMillis() - t));
        return createOkResponse("ok");
    }

    @GET
    @Path("/{fileId}/download")
    @ApiOperation(value = "File download", position = 5)
    public Response download(@PathParam(value = "fileId") @FormDataParam("fileId") String fileIdStr) {
        try {
            DataInputStream stream;
            int fileId = catalogManager.getFileId(fileIdStr);
            QueryResult<File> queryResult = catalogManager.getFile(fileId, this.queryOptions, sessionId);
            File file = queryResult.getResult().get(0);
            stream = catalogManager.downloadFile(fileId, sessionId);
//             String content = org.apache.commons.io.IOUtils.toString(stream);
            return createOkResponse(stream, MediaType.APPLICATION_OCTET_STREAM_TYPE, file.getName());
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{fileId}/content")
    @ApiOperation(value = "File content", position = 6)
    public Response content(@PathParam(value = "fileId") @FormDataParam("fileId") String fileIdStr,
                            @ApiParam(value = "start", required = false) @QueryParam("start") @DefaultValue("-1") int start,
                            @ApiParam(value = "limit", required = false) @QueryParam("limit") @DefaultValue("-1") int limit) {
        try {
            int fileId = catalogManager.getFileId(fileIdStr);
            DataInputStream stream = catalogManager.downloadFile(fileId, start, limit, sessionId);
//             String content = org.apache.commons.io.IOUtils.toString(stream);
            return createOkResponse(stream, MediaType.TEXT_PLAIN_TYPE);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{fileId}/content-grep")
    @ApiOperation(value = "File content", position = 7)
    public Response downloadGrep(
            @PathParam(value = "fileId") @FormDataParam("fileId") String fileIdStr,
            @ApiParam(value = "pattern", required = false) @QueryParam("pattern") @DefaultValue(".*") String pattern,
            @ApiParam(value = "ignoreCase", required = false) @QueryParam("ignoreCase") @DefaultValue("false") Boolean ignoreCase,
            @ApiParam(value = "multi", required = false) @QueryParam("multi") @DefaultValue("true") Boolean multi) {
        try {
            int fileId = catalogManager.getFileId(fileIdStr);
            DataInputStream stream = catalogManager.grepFile(fileId, pattern, ignoreCase, multi, sessionId);
//             String content = org.apache.commons.io.IOUtils.toString(stream);
            return createOkResponse(stream, MediaType.TEXT_PLAIN_TYPE);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/content-example")
    @ApiOperation(value = "File content", position = 8)
    public Response downloadExample(@ApiParam(value = "toolName", required = true) @DefaultValue("") @QueryParam("toolName") String toolName,
                                    @ApiParam(value = "fileName", required = true) @DefaultValue("") @QueryParam("fileName") String fileName) {
        /** I think this next two lines should be parametrized either in analysis.properties or the manifest.json of each tool **/
        String analysisPath = Config.getOpenCGAHome() + "/" + Config.getAnalysisProperties().getProperty("OPENCGA.ANALYSIS.BINARIES.PATH");
        String fileExamplesToolPath = analysisPath + "/" + toolName + "/examples/" + fileName;
        try {
            InputStream stream = new FileInputStream(fileExamplesToolPath);
            return createOkResponse(stream, MediaType.APPLICATION_OCTET_STREAM_TYPE, fileName);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/download-example")
    @ApiOperation(value = "File download", position = 9)
    public Response downloadExampleFile(@ApiParam(value = "toolName", required = true) @DefaultValue("") @QueryParam("toolName") String toolName,
                                        @ApiParam(value = "fileName", required = true) @DefaultValue("") @QueryParam("fileName") String fileName) {
        try {
            String analysisPath = Config.getGcsaHome() + "/" + Config.getAnalysisProperties().getProperty("OPENCGA.ANALYSIS.BINARIES.PATH");
            String fileExamplesToolPath = analysisPath + "/" + toolName + "/examples/" + fileName;
            InputStream istream = new FileInputStream(fileExamplesToolPath);
            DataInputStream stream = new DataInputStream(istream);
            return createOkResponse(stream, MediaType.APPLICATION_OCTET_STREAM_TYPE, fileName);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{fileId}/set-header")
    @ApiOperation(value = "Set file header", position = 10)
    public Response setHeader(@PathParam(value = "fileId") @FormDataParam("fileId") int fileId,
                              @ApiParam(value = "header", required = true) @DefaultValue("") @QueryParam("header") String header) {
        String content = "";
        DataInputStream stream;
        QueryResult<File> fileQueryResult;
        InputStream streamBody = null;
//        System.out.println("header: "+header);
        try {
            /** Obtain file uri **/
            File file = catalogManager.getFile(catalogManager.getFileId(String.valueOf(fileId)), sessionId).getResult().get(0);
            URI fileUri = catalogManager.getFileUri(file);
            System.out.println("getUri: " + fileUri.getPath());

            /** Set header **/
            stream = catalogManager.downloadFile(fileId, sessionId);
            content = org.apache.commons.io.IOUtils.toString(stream);
            String lines[] = content.split(System.getProperty("line.separator"));
            StringBuilder body = new StringBuilder();
            body.append(header);
            body.append(System.getProperty("line.separator"));
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (!line.startsWith("#")) {
                    body.append(line);
                    if (i != lines.length - 1)
                        body.append(System.getProperty("line.separator"));
                }
            }
            /** Write/Copy  file **/
            streamBody = new ByteArrayInputStream(body.toString().getBytes(StandardCharsets.UTF_8));
            Files.copy(streamBody, Paths.get(fileUri), StandardCopyOption.REPLACE_EXISTING);

        } catch (Exception e) {
            return createErrorResponse(e);
        }
//        createOkResponse(content, MediaType.TEXT_PLAIN)
        return createOkResponse(streamBody, MediaType.TEXT_PLAIN_TYPE);
    }

    @GET
    @Path("/{folderId}/files")
    @ApiOperation(value = "File content", position = 11)
    public Response getAllFilesInFolder(@PathParam(value = "folderId") @FormDataParam("folderId") String folderIdStr) {
        QueryResult<File> results;
        try {
            int folderId = catalogManager.getFileId(folderIdStr);
            results = catalogManager.getAllFilesInFolder(folderId, queryOptions, sessionId);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
        return createOkResponse(results);
    }

    @GET
    @Path("/search")
    @ApiOperation(value = "File info", position = 12)
    public Response search(@ApiParam(value = "id", required = false) @DefaultValue("") @QueryParam("id") String id,
                           @ApiParam(value = "studyId", required = true) @DefaultValue("") @QueryParam("studyId") String studyId,
                           @ApiParam(value = "name", required = false) @DefaultValue("") @QueryParam("name") String name,
                           @ApiParam(value = "type", required = false) @DefaultValue("") @QueryParam("type") String type,
                           @ApiParam(value = "path", required = false) @DefaultValue("") @QueryParam("path") String path,
                           @ApiParam(value = "bioformat", required = false) @DefaultValue("") @QueryParam("bioformat") String bioformat,
                           @ApiParam(value = "status", required = false) @DefaultValue("") @QueryParam("status") String status,
                           @ApiParam(value = "maxSize", required = false) @DefaultValue("") @QueryParam("maxSize") String maxSize,
                           @ApiParam(value = "minSize", required = false) @DefaultValue("") @QueryParam("minSize") String minSize,
                           @ApiParam(value = "startDate", required = false) @DefaultValue("") @QueryParam("startDate") String startDate,
                           @ApiParam(value = "endDate", required = false) @DefaultValue("") @QueryParam("endDate") String endDate,
                           @ApiParam(value = "like", required = false) @DefaultValue("") @QueryParam("like") String like,
                           @ApiParam(value = "startsWith", required = false) @DefaultValue("") @QueryParam("startsWith") String startsWith,
                           @ApiParam(value = "directory", required = false) @DefaultValue("") @QueryParam("directory") String directory,
                           @ApiParam(value = "attributes", required = false) @DefaultValue("") @QueryParam("attributes") String attributes) {
        try {
            int studyIdNum = catalogManager.getStudyId(studyId);

            // TODO this must be changed: only one queryOptions need to be passed
            QueryOptions query = new QueryOptions();
            for (CatalogFileDBAdaptor.FileFilterOption option : CatalogFileDBAdaptor.FileFilterOption.values()) {
                if (params.containsKey(option.name())) {
                    query.put(option.name(), params.getFirst(option.name()));
                }
            }

            QueryResult<File> result = catalogManager.searchFile(studyIdNum, query, this.queryOptions, sessionId);
            return createOkResponse(result);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{fileId}/list")
    @ApiOperation(value = "List folder", position = 13)
    public Response list(@PathParam(value = "fileId") @DefaultValue("") @FormDataParam("fileId") String fileId) {
        try {
            int fileIdNum = catalogManager.getFileId(fileId);
            QueryResult result = catalogManager.getAllFilesInFolder(fileIdNum, this.queryOptions, sessionId);
            return createOkResponse(result);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{fileId}/index")
    @ApiOperation(value = "File index", position = 14)
    public Response index(@ApiParam("fileId") @PathParam(value = "fileId") @DefaultValue("") String fileIdStr,
                          @ApiParam("Output directory id") @DefaultValue("-1") @QueryParam("outdir") String outDirStr,
                          @ApiParam("Annotate variants") @DefaultValue("true") @QueryParam("annotate") boolean annotate) {
        AnalysisFileIndexer analysisFileIndexer = new AnalysisFileIndexer(catalogManager);

        try {
            int outDirId = catalogManager.getFileId(outDirStr);
            int fileId = catalogManager.getFileId(fileIdStr);
            if(outDirId < 0) {
                outDirId = catalogManager.getFileParent(fileId, null, sessionId).first().getId();
            }
            if (!queryOptions.containsKey(AnalysisFileIndexer.PARAMETERS)) {
                File a = catalogManager.getFile(fileId, sessionId).getResult().get(0);
                if(a.getBioformat() == File.Bioformat.VARIANT){
                    queryOptions.put(AnalysisFileIndexer.PARAMETERS, Arrays.asList("--include-genotypes", "--calculate-stats", "--include-stats"));
                }
            }
            QueryResult<Job> queryResult = analysisFileIndexer.index(fileId, outDirId, sessionId, queryOptions);
            return createOkResponse(queryResult);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{fileId}/fetch")
    @ApiOperation(value = "File fetch", position = 15)
    @Deprecated
    public Response fetch(@PathParam(value = "fileId") @DefaultValue("") @FormDataParam("fileId") String fileIds,
                          @ApiParam(value = "region", allowMultiple = true, required = true) @DefaultValue("") @QueryParam("region") String region,
                          @ApiParam(value = "view_as_pairs", required = false) @DefaultValue("false") @QueryParam("view_as_pairs") boolean view_as_pairs,
                          @ApiParam(value = "include_coverage", required = false) @DefaultValue("true") @QueryParam("include_coverage") boolean include_coverage,
                          @ApiParam(value = "process_differences", required = false) @DefaultValue("true") @QueryParam("process_differences") boolean process_differences,
                          @ApiParam(value = "histogram", required = false) @DefaultValue("false") @QueryParam("histogram") boolean histogram,
                          @ApiParam(value = "variantSource", required = false) @DefaultValue("false") @QueryParam("variantSource") boolean variantSource,
                          @ApiParam(value = "interval", required = false) @DefaultValue("2000") @QueryParam("interval") int interval) {
        List<Region> regions = new LinkedList<>();
        String[] splitFileId = fileIds.split(",");
        List<Object> results = new LinkedList<>();
        for (String r : region.split(",")) {
            regions.add(new Region(r));
        }

        for (String fileId : splitFileId) {
            int fileIdNum;
            File file;
            URI fileUri;

            try {
                fileIdNum = catalogManager.getFileId(fileId);
                QueryResult<File> queryResult = catalogManager.getFile(fileIdNum, sessionId);
                file = queryResult.getResult().get(0);
                fileUri = catalogManager.getFileUri(file);
            } catch (CatalogException e) {
                e.printStackTrace();
                return createErrorResponse(e);
            }

//            if (!file.getType().equals(File.Type.INDEX)) {
            if (file.getIndex() == null || file.getIndex().getStatus() != Index.Status.READY) {
                return createErrorResponse("", "File {id:" + file.getId() + " name:'" + file.getName() + "'} " +
                        " is not an indexed file.");
            }
//            List<Index> indices = file.getIndices();
//            Index index = null;
//            for (Index i : indices) {
//                if (i.getStorageEngine().equals(backend)) {
//                    index = i;
//                }
//            }
            ObjectMap indexAttributes = new ObjectMap(file.getIndex().getAttributes());
            DataStore dataStore = null;
            try {
                dataStore = AnalysisFileIndexer.getDataStore(catalogManager, file, sessionId);
            } catch (CatalogException e) {
                e.printStackTrace();
                return createErrorResponse(e);
            }
            String storageEngine = dataStore.getStorageEngine();
            String dbName = dataStore.getDbName();
            QueryResult result;
            switch (file.getBioformat()) {
                case ALIGNMENT: {
                    //TODO: getChunkSize from file.index.attributes?  use to be 200
                    int chunkSize = indexAttributes.getInt("coverageChunkSize", 200);
                    QueryOptions queryOptions = new QueryOptions();
                    queryOptions.put(AlignmentDBAdaptor.QO_FILE_ID, Integer.toString(fileIdNum));
                    queryOptions.put(AlignmentDBAdaptor.QO_BAM_PATH, fileUri.getPath());     //TODO: Make uri-compatible
                    queryOptions.put(AlignmentDBAdaptor.QO_VIEW_AS_PAIRS, view_as_pairs);
                    queryOptions.put(AlignmentDBAdaptor.QO_INCLUDE_COVERAGE, include_coverage);
                    queryOptions.put(AlignmentDBAdaptor.QO_PROCESS_DIFFERENCES, process_differences);
                    queryOptions.put(AlignmentDBAdaptor.QO_INTERVAL_SIZE, interval);
                    queryOptions.put(AlignmentDBAdaptor.QO_HISTOGRAM, histogram);
                    queryOptions.put(AlignmentDBAdaptor.QO_COVERAGE_CHUNK_SIZE, chunkSize);

                    if (indexAttributes.containsKey("baiFileId")) {
                        File baiFile = null;
                        try {
                            baiFile = catalogManager.getFile(indexAttributes.getInt("baiFileId"), sessionId).getResult().get(0);
                            URI baiUri = catalogManager.getFileUri(baiFile);
                            queryOptions.put(AlignmentDBAdaptor.QO_BAI_PATH, baiUri.getPath());  //TODO: Make uri-compatible
                        } catch (CatalogException e) {
                            e.printStackTrace();
                            logger.error("Can't obtain bai file for file " + fileIdNum, e);
                        }
                    }

                    AlignmentDBAdaptor dbAdaptor;
                    try {
                        AlignmentStorageManager alignmentStorageManager = StorageManagerFactory.getAlignmentStorageManager(storageEngine);
                        dbAdaptor = alignmentStorageManager.getDBAdaptor(dbName, new ObjectMap());
                    } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | StorageManagerException e) {
                        return createErrorResponse(e);
                    }
                    QueryResult alignmentsByRegion;
                    if (histogram) {
                        if (regions.size() != 1) {
                            return createErrorResponse("", "Histogram fetch only accepts one region.");
                        }
                        alignmentsByRegion = dbAdaptor.getAllIntervalFrequencies(regions.get(0), queryOptions);
                    } else {
                        alignmentsByRegion = dbAdaptor.getAllAlignmentsByRegion(regions, queryOptions);
                    }
                    result = alignmentsByRegion;
                    break;
                }

                case VARIANT: {
                    QueryOptions queryOptions = new QueryOptions();
                    for (Map.Entry<String, List<String>> entry : params.entrySet()) {
                        List<String> values = entry.getValue();
                        String csv = values.get(0);
                        for (int i = 1; i < values.size(); i++) {
                            csv += "," + values.get(i);
                        }
                        queryOptions.add(entry.getKey(), csv);
                    }
                    queryOptions.put("files", Arrays.asList(Integer.toString(fileIdNum)));

                    if(params.containsKey("fileId")) {
                        if(params.get("fileId").get(0).isEmpty()) {
                            queryOptions.put("fileId", fileId);
                        } else {
                            List<String> files = params.get("fileId");
                            queryOptions.put("fileId", files.get(0));
                        }
                    }
//                    queryOptions.put("exclude", Arrays.asList(exclude.split(",")));
//                    queryOptions.put("include", Arrays.asList(include.split(",")));

                    //java.nio.file.Path configPath = Paths.get(Config.getGcsaHome(), "config", "application.properties");
                    VariantDBAdaptor dbAdaptor;
                    try {
                        dbAdaptor = StorageManagerFactory.getVariantStorageManager(storageEngine).getDBAdaptor(dbName, new ObjectMap());
//                        dbAdaptor = new CatalogVariantDBAdaptor(catalogManager, dbAdaptor);
                    } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | StorageManagerException e) {
                        return createErrorResponse(e);
                    }
                    QueryResult variantsByRegion;
                    if (histogram) {
                        queryOptions.put("interval", interval);
                        variantsByRegion = dbAdaptor.getAllVariants(queryOptions);
//                    } else if (variantSource) {
//                        queryOptions.put("fileId", Integer.toString(fileIdNum));
//                        variantsByRegion = dbAdaptor.getVariantSourceDBAdaptor().getAllSources(queryOptions);
                    } else {
                        //With merge = true, will return only one result.
                        queryOptions.put("merge", true);
                        variantsByRegion = dbAdaptor.getAllVariantsByRegionList(regions, queryOptions).get(0);
                    }
                    result = variantsByRegion;
                    break;

                }
                default:
                    return createErrorResponse("", "Unknown bioformat '" + file.getBioformat() + '\'');
            }

            result.setId(Integer.toString(fileIdNum));
            System.out.println("result = " + result);
            results.add(result);
        }
        System.out.println("results = " + results);
        return createOkResponse(results);
    }

    @GET
    @Path("/{fileId}/variants")
    @ApiOperation(value = "Fetch variants from a VCF/gVCF file", position = 15)
    public Response getVariants(@ApiParam(value = "fileId", required = true) @PathParam("fileId") String fileId) {
        return createOkResponse("PENDING");
    }

    @GET
    @Path("/{fileId}/alignments")
    @ApiOperation(value = "Fetch alignments from a BAM file", position = 15)
    public Response getAlignments(@ApiParam(value = "fileId", required = true) @PathParam("fileId") String fileId) {
        return createOkResponse("PENDING");
    }

    private ObjectMap getResumeFileJSON(java.nio.file.Path folderPath) throws IOException {
        ObjectMap objectMap = new ObjectMap();

        if (Files.exists(folderPath)) {
            DirectoryStream<java.nio.file.Path> folderStream = Files.newDirectoryStream(folderPath, "*_partial");
            for (java.nio.file.Path partPath : folderStream) {
                String[] nameSplit = partPath.getFileName().toString().split("_");
                ObjectMap chunkInfo = new ObjectMap();
                chunkInfo.put("size", Integer.parseInt(nameSplit[1]));
                objectMap.put(nameSplit[0], chunkInfo);
            }
        }
        return objectMap;
    }

    private List<java.nio.file.Path> getSortedChunkList(java.nio.file.Path folderPath) throws IOException {
        List<java.nio.file.Path> files = new ArrayList<>();
        DirectoryStream<java.nio.file.Path> stream = Files.newDirectoryStream(folderPath, "*_partial");
        for (java.nio.file.Path p : stream) {
            logger.info("adding to ArrayList: " + p.getFileName());
            files.add(p);
        }
        logger.info("----ordered files length: " + files.size());
        Collections.sort(files, new Comparator<java.nio.file.Path>() {
            public int compare(java.nio.file.Path o1, java.nio.file.Path o2) {
                int id_o1 = Integer.parseInt(o1.getFileName().toString().split("_")[0]);
                int id_o2 = Integer.parseInt(o2.getFileName().toString().split("_")[0]);
                return id_o1 - id_o2;
            }
        });
        return files;
    }

    @GET
    @Path("/{fileId}/update")
    @ApiOperation(value = "Modify file", position = 16)
    public Response update(@PathParam(value = "fileId") @FormDataParam("fileId") String fileIdStr) {
        try {
            ObjectMap parameters = new ObjectMap();
            for (String param : params.keySet()) {
                if (param.equalsIgnoreCase("sid"))
                    continue;
                String value = params.get(param).get(0);
                parameters.put(param, value);
            }
            int fileId = catalogManager.getFileId(fileIdStr);
            QueryResult queryResult = catalogManager.modifyFile(fileId, parameters, sessionId);
            return createOkResponse(queryResult);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{fileId}/delete")
    @ApiOperation(value = "Delete file", position = 17)
    public Response deleteGET(@PathParam(value = "fileId") @DefaultValue("") @FormDataParam("fileId") String fileId) {
        try {
            int fileIdNum = catalogManager.getFileId(fileId);
            QueryResult result = catalogManager.deleteFile(fileIdNum, sessionId);
            return createOkResponse(result);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

}
