package org.opencb.opencga.server.ws;

import org.opencb.commons.containers.QueryResult;
import org.opencb.opencga.account.db.AccountManagementException;
import org.opencb.opencga.account.io.IOManagementException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

@Path("/user/{userId}")
public class UserWSServer extends GenericWSServer {
    private String userId;

    public UserWSServer(@Context UriInfo uriInfo, @Context HttpServletRequest httpServletRequest,
                        @DefaultValue("") @PathParam("userId") String userId) throws IOException, AccountManagementException {
        super(uriInfo, httpServletRequest);
        this.userId = userId;
    }


    @POST
    @Path("/create")
    public Response createPost(@DefaultValue("") @QueryParam("password") String password,
                           @DefaultValue("") @QueryParam("name") String name, @DefaultValue("") @QueryParam("email") String email) throws IOException {
        return null;
    }
    @GET
    @Path("/create")
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
    @Path("/login")
    public Response login(@DefaultValue("") @QueryParam("password") String password) throws IOException {
//        try {
//            QueryResult result;
//            if (userId.toLowerCase().equals("anonymous")) {
//                System.out.println("TEST ERROR userId = " + userId);
//                result = cloudSessionManager.createAnonymousAccount(sessionIp);
//            } else {
//                result = cloudSessionManager.login(userId, password, sessionIp);
//            }
//            return createOkResponse(result);
//        } catch (AccountManagementException | IOManagementException e) {
//            logger.error(e.toString());
//            return createErrorResponse("could not login");
//        }
        return null;
    }

    @GET
    @Path("/logout")
    public Response logout() throws IOException {
//        try {
//            QueryResult result;
//            if (userId.toLowerCase().equals("anonymous")) {
//                result = cloudSessionManager.logoutAnonymous(sessionId);
//            } else {
//                result = cloudSessionManager.logout(userId, sessionId);
//            }
//            return createOkResponse(result);
//        } catch (AccountManagementException | IOManagementException e) {
//            logger.error(e.toString());
//            return createErrorResponse("could not logout");
//        }
        return null;
    }

    @GET
    @Path("/info")
    public Response getInfoAccount(@DefaultValue("") @QueryParam("last_activity") String lastActivity) {
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
    @Path("/delete/")
    public Response deleteAccount() {
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
    @Path("/project/list")
    public Response list() {
        return null;

    }


}