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

package org.opencb.opencga.server;


//@Path("/{version}/analysis")
//@Api(value = "analysis", description = "analysis")
@Deprecated
public class AnalysisWSServer {

//    public AnalysisWSServer(@PathParam("version") String version, @Context UriInfo uriInfo, @Context HttpServletRequest httpServletRequest) throws IOException {
//        super(version, uriInfo, httpServletRequest);
//    }

//    @GET
//    @Path("/create")
//    @Produces("application/json")
//    @ApiOperation(value = "Create analysis")
//
//    public Response createStudy(
//            @ApiParam(value = "studyId", required = true) @QueryParam("studyId") int studyId,
//            @ApiParam(value = "name", required = true) @QueryParam("name") String name,
//            @ApiParam(value = "alias", required = true) @QueryParam("alias") String alias,
//            @ApiParam(value = "creatorId", required = true) @QueryParam("creatorId") String creatorId,
//            @ApiParam(value = "description", required = true) @QueryParam("description") String description
//    ) {
//
//
//        QueryResult queryResult;
//        try {
//            queryResult = catalogManager.createAnalysis(studyId, name, alias, description, sessionId);
//
////            queryResult = catalogManager.createStudy(projectId, name, alias, type, description, sessionId);
//
//            return createOkResponse(queryResult);
//
//        } catch (CatalogManagerException e) {
//            e.printStackTrace();
//            return createErrorResponse(e.getMessage());
//        }
//
//    }
//
//    @GET
//    @Path("/{analysisId}/info")
//    @Produces("application/json")
//    @ApiOperation(value = "Get analysisInfo")
//    public Response info(
//            @ApiParam(value = "analysisId", required = true) @PathParam("analysisId") int analysisId) {
//        try {
//            return createOkResponse(catalogManager.getAnalysis(analysisId, sessionId));
//        } catch (CatalogManagerException e) {
//            return createErrorResponse(e.getMessage());
//        }
//    }
//
//    @GET
//    @Path("/{analysisId}/jobs")
//    @Produces("application/json")
//    @ApiOperation(value = "Get all jobs")
//    public Response getAllJobs(
//            @ApiParam(value = "analysisId", required = true) @PathParam("analysisId") int analysisId) {
//        try {
//            return createOkResponse(catalogManager.getJobsByAnalysis(analysisId, sessionId));
//        } catch (CatalogManagerException e) {
//            return createErrorResponse(e.getMessage());
//        }
//    }

}