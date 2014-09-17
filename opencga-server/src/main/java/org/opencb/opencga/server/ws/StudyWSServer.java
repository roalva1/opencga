package org.opencb.opencga.server.ws;

import org.opencb.opencga.account.db.AccountManagementException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

@Path("/user/{userId}/project/{projectId}/study")
public class StudyWSServer extends GenericWSServer {
    private String userId;
    private String projectId;
    private String studyId;

    public StudyWSServer(@Context UriInfo uriInfo, @Context HttpServletRequest httpServletRequest,
                         @DefaultValue("") @PathParam("userId") String userId,
                         @DefaultValue("") @PathParam("projectId") String projectId,
                         @DefaultValue("") @PathParam("projectId") String studyId) throws IOException, AccountManagementException {
        super(uriInfo, httpServletRequest);
        this.userId = userId;
        this.projectId = projectId;
        this.studyId = studyId;
    }


    @POST
    @Path("/{studyId}/create")
    public Response createPost(@DefaultValue("") @QueryParam("password") String password,
                           @DefaultValue("") @QueryParam("name") String name, @DefaultValue("") @QueryParam("email") String email) throws IOException {
        return null;
    }
    @GET
    @Path("/{studyId}/create")
    public Response create(@DefaultValue("") @QueryParam("password") String password,
                           @DefaultValue("") @QueryParam("name") String name, @DefaultValue("") @QueryParam("email") String email) throws IOException {

//        QueryResult result;
//        try {
//            if (userId.toLowerCase().equals("anonymous")) {
//                result = cloudSessionManager.createAnonymouAccount(sessionIp);
//            } else {
//                result = cloudSessionManager.createAccount(userId, password, name, email, sessionIp);
//            }
//            return createOkResponse(result);
//        } catch (AccountManagementException | IOManagementException e) {
//            logger.error(e.toString());
//            return createErrorResponse("could not create the account");
//        }
        return null;
    }

    @GET
    @Path("/{studyId}/info")
    public Response getInfo(@DefaultValue("") @QueryParam("last_activity") String lastActivity) {
//        try {
//            QueryResult result = cloudSessionManager.getAccountInfo(userId, lastActivity, sessionId);
//            return createOkResponse(result);
//        } catch (AccountManagementException e) {
//            logger.error(userId);
//            logger.error(e.toString());
//            return createErrorResponse("could not get account information");
//        }
        return null;
    }

    @GET
    @Path("/{studyId}/delete/")
    public Response delete() {
//    try {
//        cloudSessionManager.deleteAccount(userId, sessionId);
//    return createOkResponse("OK");
//    } catch (AccountManagementException e) {
//    logger.error(e.toString());
//    return createErrorResponse("could not delete the account");
//    }
        return null;
    }
    @GET
    @Path("/{studyId}/list_files")
    public Response listFile() {
        return null;

    }

    @GET
    @Path("/{studyId}/add_file")
    public Response addFile() {
        return null;

    }

    @GET
    @Path("/{studyId}/delete_file")
    public Response deleteFile() {
        return null;

    }

    @GET
    @Path("/{studyId}/edit_file")
    public Response editFile() {
        return null;

    }


    @GET
    @Path("/list")
    public Response list() {
        return null;

    }


}