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

package org.opencb.opencga.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.opencb.datastore.core.ObjectMap;
import org.opencb.datastore.core.QueryOptions;
import org.opencb.datastore.core.QueryResult;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.CatalogManager;
import org.opencb.opencga.catalog.models.File;
import org.opencb.opencga.catalog.models.Job;
import org.opencb.opencga.core.SgeManager;
import org.opencb.opencga.core.common.Config;
import org.opencb.opencga.analysis.beans.Analysis;
import org.opencb.opencga.analysis.beans.Execution;
import org.opencb.opencga.analysis.beans.Option;
import org.opencb.opencga.core.common.StringUtils;
import org.opencb.opencga.core.common.TimeUtils;
import org.opencb.opencga.core.exec.Command;
import org.opencb.opencga.core.exec.SingleProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class AnalysisJobExecutor {

    public static final String EXECUTE = "execute";
    public static final String SIMULATE = "simulate";
    public static final String OPENCGA_ANALYSIS_JOB_EXECUTOR = "OPENCGA.ANALYSIS.JOB.EXECUTOR";
    protected static Logger logger = LoggerFactory.getLogger(AnalysisJobExecutor.class);
    protected final Properties analysisProperties;
    protected final String home;
    protected String analysisName;
    protected String executionName;
    protected Path analysisRootPath;
    protected Path analysisPath;
    protected Path manifestFile;
    protected Path resultsFile;
    protected String sessionId;
    protected Analysis analysis;
    protected Execution execution;

    protected static ObjectMapper jsonObjectMapper = new ObjectMapper();

    private AnalysisJobExecutor() throws IOException, AnalysisExecutionException {
        home = Config.getOpenCGAHome();
        analysisProperties = Config.getAnalysisProperties();
        executionName = null;
    }

    public AnalysisJobExecutor(String analysisStr, String execution) throws IOException, AnalysisExecutionException {
        this(analysisStr, execution, "system");
    }

    @Deprecated
    public AnalysisJobExecutor(String analysisStr, String execution, String analysisOwner) throws IOException, AnalysisExecutionException {
        this();
        if (analysisOwner.equals("system")) {
            this.analysisRootPath = Paths.get(analysisProperties.getProperty("OPENCGA.ANALYSIS.BINARIES.PATH"));
        } else {
            this.analysisRootPath = Paths.get(home, "accounts", analysisOwner);
        }
        this.analysisName = analysisStr;
        if (analysisName.contains(".")) {
            executionName = analysisName.split("\\.")[1];
            analysisName = analysisName.split("\\.")[0];
        } else {
            executionName = execution;
        }

        load();
    }

    public AnalysisJobExecutor(Path analysisRootPath, String analysisName, String executionName) throws IOException, AnalysisExecutionException {
        this();
        this.analysisRootPath = analysisRootPath;
        this.analysisName = analysisName;
        this.executionName = executionName;

        load();
    }

    private void load() throws IOException, AnalysisExecutionException {

        analysisPath = Paths.get(home).resolve(analysisRootPath).resolve(analysisName);
        manifestFile = analysisPath.resolve(Paths.get("manifest.json"));
        resultsFile = analysisPath.resolve(Paths.get("results.js"));

        analysis = getAnalysis();
        execution = getExecution();
    }

    public void execute(String jobName, int jobId, String jobFolder, String commandLine) throws AnalysisExecutionException, IOException {
        logger.debug("AnalysisJobExecuter: execute, 'jobName': " + jobName + ", 'jobFolder': " + jobFolder);
        logger.debug("AnalysisJobExecuter: execute, command line: " + commandLine);

        executeCommandLine(commandLine, jobName, jobId, jobFolder, analysisName);
    }

    public static void execute(CatalogManager catalogManager, Job job, String sessionId)
            throws AnalysisExecutionException, IOException, CatalogException {
        logger.debug("AnalysisJobExecuter: execute, job: {}", job);

        // read execution param
        String jobExecutor = Config.getAnalysisProperties().getProperty(OPENCGA_ANALYSIS_JOB_EXECUTOR);

        // local execution
        if (jobExecutor == null || jobExecutor.trim().equalsIgnoreCase("LOCAL")) {
            logger.debug("AnalysisJobExecuter: execute, running by SingleProcess");
            executeLocal(catalogManager, job, sessionId);
        } else {
            logger.debug("AnalysisJobExecuter: execute, running by SgeManager");

            try {
                SgeManager.queueJob(job.getToolName(), job.getResourceManagerAttributes().get(Job.JOB_SCHEDULER_NAME).toString(),
                        -1, job.getTmpOutDirUri().getPath(), job.getCommandLine(), null, "job." + job.getId());
                catalogManager.modifyJob(job.getId(), new ObjectMap("status", Job.Status.QUEUED), sessionId);
            } catch (Exception e) {
                logger.error(e.toString());
                throw new AnalysisExecutionException("ERROR: sge execution failed.");
            }
        }
    }

    private boolean checkRequiredParams(Map<String, List<String>> params, List<Option> validParams) {
        for (Option param : validParams) {
            if (param.isRequired() && !params.containsKey(param.getName())) {
                System.out.println("Missing param: " + param);
                return false;
            }
        }
        return true;
    }

    private Map<String, List<String>> removeUnknownParams(Map<String, List<String>> params, List<Option> validOptions) {
        Set<String> validKeyParams = new HashSet<String>();
        for (Option param : validOptions) {
            validKeyParams.add(param.getName());
        }

        Map<String, List<String>> paramsCopy = new HashMap<String, List<String>>(params);
        for (String param : params.keySet()) {
            if (!validKeyParams.contains(param)) {
                paramsCopy.remove(param);
            }
        }

        return paramsCopy;
    }

    public String createCommandLine(Map<String, List<String>> params)
            throws AnalysisExecutionException {
        return createCommandLine(execution.getExecutable(), params);
    }

    public String createCommandLine(String executable, Map<String, List<String>> params)
            throws AnalysisExecutionException {
        logger.debug("params received in createCommandLine: " + params);
        String binaryPath = analysisPath.resolve(executable).toString();

        // Check required params
        List<Option> validParams = execution.getValidParams();
        validParams.addAll(analysis.getGlobalParams());
        validParams.add(new Option(execution.getOutputParam(), "Outdir", false));
        if (checkRequiredParams(params, validParams)) {
            params = new HashMap<String, List<String>>(removeUnknownParams(params, validParams));
        } else {
            throw new AnalysisExecutionException("ERROR: missing some required params.");
        }

        StringBuilder cmdLine = new StringBuilder();
        cmdLine.append(binaryPath);

        if (params.containsKey("tool")) {
            String tool = params.get("tool").get(0);
            cmdLine.append(" --tool ").append(tool);
            params.remove("tool");
        }

        for (String key : params.keySet()) {
            // Removing renato param
            if (!key.equals("renato")) {
                if (key.length() == 1) {
                    cmdLine.append(" -").append(key);
                } else {
                    cmdLine.append(" --").append(key);
                }
                if (params.get(key) != null) {
                    String paramsArray = params.get(key).toString();
                    String paramValue = paramsArray.substring(1, paramsArray.length() - 1).replaceAll("\\s", "");
                    cmdLine.append(" ").append(paramValue);
                }
            }
        }
        return cmdLine.toString();
    }

    public QueryResult<Job> createJob(Map<String, List<String>> params,
                                      CatalogManager catalogManager, int studyId, String jobName, String description, File outDir, List<Integer> inputFiles, String sessionId)
            throws AnalysisExecutionException, CatalogException {
        return createJob(execution.getExecutable(), params, catalogManager, studyId, jobName, description, outDir, inputFiles, sessionId);
    }

    public QueryResult<Job> createJob(String executable, Map<String, List<String>> params,
                                      CatalogManager catalogManager, int studyId, String jobName, String description, File outDir,
                                      List<Integer> inputFiles, String sessionId)
            throws AnalysisExecutionException, CatalogException {

        // Create temporal Outdir
        String randomString = "J_" + StringUtils.randomString(10);
        URI temporalOutDirUri = catalogManager.createJobOutDir(studyId, randomString, sessionId);
        params.put(getExecution().getOutputParam(), Arrays.asList(temporalOutDirUri.getPath()));

        // Create commandLine
        String commandLine = createCommandLine(executable, params);
        System.out.println(commandLine);

        return createJob(catalogManager, studyId, jobName, analysisName, description, outDir, inputFiles, sessionId,
                randomString, temporalOutDirUri, commandLine, false, false, new HashMap<String, Object>(), new HashMap<String, Object>());
    }

    public static QueryResult<Job> createJob(CatalogManager catalogManager, int studyId, String jobName, String toolName, String description,
                                             File outDir, List<Integer> inputFiles, String sessionId,
                                             String randomString, URI temporalOutDirUri, String commandLine,
                                             boolean execute, boolean simulate, Map<String, Object> attributes,
                                             Map<String, Object> resourceManagerAttributes)
            throws AnalysisExecutionException, CatalogException {
        logger.debug("Creating job {}: simulate {}, execute {}", jobName, simulate, execute);
        long start = System.currentTimeMillis();

        QueryResult<Job> jobQueryResult;
        if (resourceManagerAttributes == null) {
            resourceManagerAttributes = new HashMap<>();
        }
        if (simulate) { //Simulate a job. Do not create it.
            resourceManagerAttributes.put(Job.JOB_SCHEDULER_NAME, randomString);
            jobQueryResult = new QueryResult<>("simulatedJob", (int) (System.currentTimeMillis() - start), 1, 1, "", "", Collections.singletonList(
                    new Job(-10, jobName, catalogManager.getUserIdBySessionId(sessionId), toolName,
                            TimeUtils.getTime(), description, start, System.currentTimeMillis(), "", commandLine, -1,
                            Job.Status.PREPARED, -1, outDir.getId(), temporalOutDirUri, inputFiles, Collections.<Integer>emptyList(),
                            null, attributes, resourceManagerAttributes)));
        } else {
            if (execute) {
                /** Create a RUNNING job in CatalogManager **/
                jobQueryResult = catalogManager.createJob(studyId, jobName, toolName, description, commandLine, temporalOutDirUri,
                        outDir.getId(), inputFiles, attributes, resourceManagerAttributes, Job.Status.RUNNING, null, sessionId);
                Job job = jobQueryResult.first();

                jobQueryResult = executeLocal(catalogManager, job, sessionId);

            } else {
                /** Create a PREPARED job in CatalogManager **/
                resourceManagerAttributes.put(Job.JOB_SCHEDULER_NAME, randomString);
                jobQueryResult = catalogManager.createJob(studyId, jobName, toolName, description, commandLine, temporalOutDirUri,
                        outDir.getId(), inputFiles, attributes, resourceManagerAttributes, Job.Status.PREPARED, null, sessionId);
            }
        }
        return jobQueryResult;
    }

    private static QueryResult<Job> executeLocal(CatalogManager catalogManager, Job job, String sessionId) throws CatalogException, IOException {
        logger.info("==========================================");
        logger.info("Executing job {}({})", job.getName(), job.getId());
        logger.debug("Executing commandLine {}", job.getCommandLine());
        logger.info("==========================================");
        System.err.println();

        String commandLine = job.getTmpOutDirUri()+"/command_line.sh";
        Files.write(Paths.get(commandLine), job.getCommandLine().getBytes());
        Command com = new Command(commandLine);
        com.run();

        System.err.println();
        logger.info("==========================================");
        logger.info("Finished job {}({})", job.getName(), job.getId());
        logger.info("==========================================");


        /** Change status to DONE  - Add the execution information to the job entry **/
//                new AnalysisJobManager().jobFinish(jobQueryResult.first(), com.getExitValue(), com);
        ObjectMap parameters = new ObjectMap();
        parameters.put("resourceManagerAttributes", new ObjectMap("executionInfo", com));
        parameters.put("status", Job.Status.DONE);
        catalogManager.modifyJob(job.getId(), parameters, sessionId);

        /** Record output **/
        AnalysisOutputRecorder outputRecorder = new AnalysisOutputRecorder(catalogManager, sessionId);
        outputRecorder.recordJobOutput(job, com.getExitValue() != 0);

        /** Change status to READY or ERROR **/
        if (com.getExitValue() == 0) {
            catalogManager.modifyJob(job.getId(), new ObjectMap("status", Job.Status.READY), sessionId);
        } else {
            parameters = new ObjectMap();
            parameters.put("status", Job.Status.ERROR);
            parameters.put("error", Job.ERRNO_FINISH_ERROR);
            parameters.put("errorDescription", Job.errorDescriptions.get(Job.ERRNO_FINISH_ERROR));
            catalogManager.modifyJob(job.getId(), parameters, sessionId);
        }

        return catalogManager.getJob(job.getId(), new QueryOptions(), sessionId);
    }

    private static void executeCommandLine(String commandLine, String jobName, int jobId, String jobFolder, String analysisName)
            throws AnalysisExecutionException, IOException {
        // read execution param
        String jobExecutor = Config.getAnalysisProperties().getProperty("OPENCGA.ANALYSIS.JOB.EXECUTOR");

        // local execution
        if (jobExecutor == null || jobExecutor.trim().equalsIgnoreCase("LOCAL")) {
            logger.debug("AnalysisJobExecuter: execute, running by SingleProcess");

            Command com = new Command(commandLine);
            SingleProcess sp = new SingleProcess(com);
            sp.getRunnableProcess().run();
        }
        // sge execution
        else {
            logger.debug("AnalysisJobExecuter: execute, running by SgeManager");

            try {
                SgeManager.queueJob(analysisName, jobName, -1, jobFolder, commandLine, null, "job." + jobId);
            } catch (Exception e) {
                logger.error(e.toString());
                throw new AnalysisExecutionException("ERROR: sge execution failed.");
            }
        }
    }

    public Analysis getAnalysis() throws IOException, AnalysisExecutionException {
        if (analysis == null) {
            analysis = jsonObjectMapper.readValue(manifestFile.toFile(), Analysis.class);
//            analysis = gson.fromJson(IOUtils.toString(manifestFile.toFile()), Analysis.class);
        }
        return analysis;
    }

    public Execution getExecution() throws AnalysisExecutionException {
        if (execution == null) {
            if (executionName == null || executionName.isEmpty()) {
                execution = analysis.getExecutions().get(0);
            } else {
                for (Execution exe : analysis.getExecutions()) {
                    if (exe.getId().equalsIgnoreCase(executionName)) {
                        execution = exe;
                        break;
                    }
                }
            }
        }
        return execution;
    }

    public String getExamplePath(String fileName) {
        return analysisPath.resolve("examples").resolve(fileName).toString();
    }

    public String help(String baseUrl) {
        if (!Files.exists(manifestFile)) {
            return "Manifest for " + analysisName + " not found.";
        }

        String execName = "";
        if (executionName != null)
            execName = "." + executionName;
        StringBuilder sb = new StringBuilder();
        sb.append("Analysis: " + analysis.getName() + "\n");
        sb.append("Description: " + analysis.getDescription() + "\n");
        sb.append("Version: " + analysis.getVersion() + "\n\n");
        sb.append("Author: " + analysis.getAuthor().getName() + "\n");
        sb.append("Email: " + analysis.getAuthor().getEmail() + "\n");
        if (!analysis.getWebsite().equals(""))
            sb.append("Website: " + analysis.getWebsite() + "\n");
        if (!analysis.getPublication().equals(""))
            sb.append("Publication: " + analysis.getPublication() + "\n");
        sb.append("\nUsage: \n");
        sb.append(baseUrl + "analysis/" + analysisName + execName + "/{action}?{params}\n\n");
        sb.append("\twhere: \n");
        sb.append("\t\t{action} = [run, help, params, test, status]\n");
        sb.append("\t\t{params} = " + baseUrl + "analysis/" + analysisName + execName + "/params\n");
        return sb.toString();
    }

    public String params() {
        if (!Files.exists(manifestFile)) {
            return "Manifest for " + analysisName + " not found.";
        }

        if (execution == null) {
            return "ERROR: Executable not found.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Valid params for " + analysis.getName() + ":\n\n");
        for (Option param : execution.getValidParams()) {
            String required = "";
            if (param.isRequired())
                required = "*";
            sb.append("\t" + param.getName() + ": " + param.getDescription() + " " + required + "\n");
        }
        sb.append("\n\t*: required parameters.\n");
        return sb.toString();
    }

    public String test(String jobName, int jobId, String jobFolder) throws AnalysisExecutionException, IOException {
        // TODO test

        if (!Files.exists(manifestFile)) {
            return "Manifest for " + analysisName + " not found.";
        }

        if (execution == null) {
            return "ERROR: Executable not found.";
        }

        executeCommandLine(execution.getTestCmd(), jobName, jobId, jobFolder, analysisName);

        return String.valueOf(jobName);
    }

    public String getResult() throws AnalysisExecutionException {
        return execution.getResult();
    }

    public InputStream getResultInputStream() throws AnalysisExecutionException, IOException {
        System.out.println(resultsFile.toAbsolutePath().toString());
        if (!Files.exists(resultsFile)) {
            resultsFile = analysisPath.resolve(Paths.get("results.js"));
        }

        if (Files.exists(resultsFile)) {
            return Files.newInputStream(resultsFile);
        }
        throw new AnalysisExecutionException("result.js not found.");
    }
}
