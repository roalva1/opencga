package org.opencb.opencga.catalog.db.api;

import org.opencb.datastore.core.ObjectMap;
import org.opencb.datastore.core.QueryOptions;
import org.opencb.datastore.core.QueryResult;
import org.opencb.opencga.catalog.models.Acl;
import org.opencb.opencga.catalog.models.Dataset;
import org.opencb.opencga.catalog.models.File;
import org.opencb.opencga.catalog.exceptions.CatalogDBException;

/**
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
public interface CatalogFileDBAdaptor {

    public enum FileFilterOption implements CatalogDBAdaptor.FilterOption {
        studyId(Type.NUMERICAL, ""),
        directory(Type.TEXT, ""),

        id(Type.NUMERICAL, ""),
        name(Type.TEXT, ""),
        type(Type.TEXT, ""),
        format(Type.TEXT, ""),
        bioformat(Type.TEXT, ""),
        uri(Type.TEXT, ""),
        path(Type.TEXT, ""),
        ownerId(Type.TEXT, ""),
        creationDate(Type.TEXT, ""),
        modificationDate(Type.TEXT, ""),
        description(Type.TEXT, ""),
        status(Type.TEXT, ""),
        diskUsage(Type.NUMERICAL, ""),
        experimentId(Type.NUMERICAL, ""),
        sampleIds(Type.NUMERICAL, ""),
        jobId(Type.NUMERICAL, ""),
        acl(Type.TEXT, ""),
        bacl("acl", Type.BOOLEAN, ""),

        stats(Type.TEXT, ""),
        nstats("stats", Type.NUMERICAL, ""),

        attributes(Type.TEXT, "Format: <key><operation><stringValue> where <operation> is [<|<=|>|>=|==|!=|~|!~]"),
        nattributes("attributes", Type.NUMERICAL, "Format: <key><operation><numericalValue> where <operation> is [<|<=|>|>=|==|!=|~|!~]"),
        battributes("attributes", Type.BOOLEAN, "Format: <key><operation><true|false> where <operation> is [==|!=]"),

        @Deprecated maxSize(Type.NUMERICAL, ""),
        @Deprecated minSize(Type.NUMERICAL, ""),
        @Deprecated startDate(Type.TEXT, ""),
        @Deprecated endDate(Type.TEXT, ""),
        @Deprecated like(Type.TEXT, ""),
        @Deprecated startsWith(Type.TEXT, ""),
        ;

        private FileFilterOption(Type type, String description) {
            this._key = name();
            this._description = description;
            this._type = type;
        }

        private FileFilterOption(String key, Type type, String description) {
            this._key = key;
            this._description = description;
            this._type = type;
        }

        final private String _key;
        final private String _description;
        final private Type _type;

        @Override
        public String getDescription() {
            return _description;
        }

        @Override
        public Type getType() {
            return _type;
        }

        @Override
        public String getKey() {
            return _key;
        }
    }

    /**
     * File methods
     * ***************************
     */

    // add file to study
    QueryResult<File> createFileToStudy(int studyId, File file, QueryOptions options) throws CatalogDBException;

    QueryResult<Integer> deleteFile(int fileId) throws CatalogDBException;

    int getFileId(int studyId, String path) throws CatalogDBException;

    QueryResult<File> getAllFiles(int studyId, QueryOptions options) throws CatalogDBException;

    QueryResult<File> getAllFilesInFolder(int folderId, QueryOptions options) throws CatalogDBException;

    QueryResult<File> getFile(int fileId, QueryOptions options) throws CatalogDBException;

    QueryResult setFileStatus(int fileId, File.Status status) throws CatalogDBException;

    QueryResult modifyFile(int fileId, ObjectMap parameters) throws CatalogDBException;

    QueryResult renameFile(int fileId, String name) throws CatalogDBException;

    int getStudyIdByFileId(int fileId) throws CatalogDBException;

    String getFileOwnerId(int fileId) throws CatalogDBException;

    QueryResult<Acl> getFileAcl(int fileId, String userId) throws CatalogDBException;

    QueryResult setFileAcl(int fileId, Acl newAcl) throws CatalogDBException;

    QueryResult<File> searchFile(QueryOptions query, QueryOptions options) throws CatalogDBException;

    QueryResult<Dataset> createDataset(int studyId, Dataset dataset, QueryOptions options) throws CatalogDBException;

    QueryResult<Dataset> getDataset(int datasetId, QueryOptions options) throws CatalogDBException;

    int getStudyIdByDatasetId(int datasetId) throws CatalogDBException;

}
