package org.opencb.opencga.server.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import encryption.AESCipher;
import encryption.KeystoreUtil;
import com.google.common.base.Splitter;
import org.opencb.datastore.core.QueryOptions;
import org.opencb.datastore.core.QueryResponse;
import org.opencb.opencga.account.CloudSessionManager;
import org.opencb.opencga.account.io.IOManagementException;
import org.opencb.opencga.lib.common.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.*;
import java.security.Key;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

@Path("/")
public class GenericWSServer {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());
    protected static Properties properties;
    protected static Config config;

    protected UriInfo uriInfo;
    protected String accountId;
    protected String sessionIp;

    // Common input arguments
    protected MultivaluedMap<String, String> params;
    protected QueryOptions queryOptions;

    // Common output members
    protected String outputFormat;
    protected long startTime;
    protected long endTime;
    protected QueryResponse queryResponse;

    protected static ObjectWriter jsonObjectWriter;
    protected static ObjectMapper jsonObjectMapper;

    //General params
    @DefaultValue("")
    @QueryParam("sessionid")
    protected String sessionId;

    @DefaultValue("json")
    @QueryParam("of")
    protected String of;



    @DefaultValue("")
    @QueryParam("enc")
    protected String enc;

    /**
     * Only one CloudSessionManager
     */
    protected static CloudSessionManager cloudSessionManager;

    static {
        try {
            cloudSessionManager = new CloudSessionManager();
        } catch (IOException | IOManagementException e) {
            e.printStackTrace();
        }

        jsonObjectMapper = new ObjectMapper();
        jsonObjectWriter = jsonObjectMapper.writer();

        InputStream is = CloudSessionManager.class.getClassLoader().getResourceAsStream("application.properties");
        properties = new Properties();
        try {
            properties.load(is);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public GenericWSServer(@Context UriInfo uriInfo, @Context HttpServletRequest httpServletRequest) throws IOException {
        this.startTime = System.currentTimeMillis();
        this.uriInfo = uriInfo;
        this.params = this.uriInfo.getQueryParameters();
        this.queryOptions = new QueryOptions();
        queryResponse = new QueryResponse();
        parseCommonQueryParameters(this.params);
        this.sessionIp = httpServletRequest.getRemoteAddr();


        logger.debug(uriInfo.getRequestUri().toString());

        File dqsDir = new File(properties.getProperty("DQS.PATH"));
        if (dqsDir.exists()) {
            File accountsDir = new File(properties.getProperty("ACCOUNTS.PATH"));
            if (!accountsDir.exists()) {
                accountsDir.mkdir();
            }
        }

        startTime = System.currentTimeMillis();

    }


    /**
     * This method parse common query parameters from the URL
     *
     * @param multivaluedMap
     */
    private void parseCommonQueryParameters(MultivaluedMap<String, String> multivaluedMap) {
        queryOptions.put("metadata", (multivaluedMap.get("metadata") != null) ? multivaluedMap.get("metadata").get(0).equals("true") : true);
        queryOptions.put("exclude", (multivaluedMap.get("exclude") != null) ? Splitter.on(",").splitToList(multivaluedMap.get("exclude").get(0)) : null);
        queryOptions.put("include", (multivaluedMap.get("include") != null) ? Splitter.on(",").splitToList(multivaluedMap.get("include").get(0)) : null);
        queryOptions.put("limit", (multivaluedMap.get("limit") != null) ? multivaluedMap.get("limit").get(0) : -1);
        queryOptions.put("skip", (multivaluedMap.get("skip") != null) ? multivaluedMap.get("skip").get(0) : -1);
        queryOptions.put("count", (multivaluedMap.get("count") != null) ? Boolean.parseBoolean(multivaluedMap.get("count").get(0)) : false);

        outputFormat = (multivaluedMap.get("of") != null) ? multivaluedMap.get("of").get(0) : "json";
    }


    @GET
    @Path("/echo/{message}")
    public Response echoGet(@PathParam("message") String message) {
        logger.info(sessionId);
        logger.info(of);
        return createOkResponse(message);
    }

    protected Response createErrorResponse(Object obj) {
        endTime = System.currentTimeMillis() - startTime;
        queryResponse.setTime(new Long(endTime - startTime).intValue());
//        queryResponse.setApiVersion(version);
        queryResponse.setQueryOptions(queryOptions);
        queryResponse.setError(obj.toString());

        // Guarantee that the QueryResponse object contains a coll of results
        Collection coll;
        if (obj instanceof Collection) {
            coll = (Collection) obj;
        } else {
            coll = new ArrayList();
            coll.add(obj);
        }
        queryResponse.setResponse(coll);

        return createJsonResponse(queryResponse);
    }

    protected Response createOkResponse(Object obj) {

        endTime = System.currentTimeMillis() - startTime;
        queryResponse.setTime(new Long(endTime - startTime).intValue());
//        queryResponse.setApiVersion(version);
        queryResponse.setQueryOptions(queryOptions);

        // Guarantee that the QueryResponse object contains a coll of results

        if (enc.equalsIgnoreCase("aes-256")){
           obj =  encryptResponse(obj);

        }
        Collection coll;
        if (obj instanceof Collection) {
            coll = (Collection) obj;
        } else {
            coll = new ArrayList();
            coll.add(obj);
        }

        queryResponse.setResponse(coll);

        switch (outputFormat.toLowerCase()) {
            case "json":
                return createJsonResponse(queryResponse);
            case "xml":
                return createOkResponse(queryResponse, MediaType.APPLICATION_XML_TYPE);
            default:
                return createJsonResponse(queryResponse);
        }
    }

    protected Response createJsonResponse(Object obj) {
//        endTime = System.currentTimeMillis() - startTime;
//        QueryResponse queryResponse = new QueryResponse(queryOptions, obj,
//                (params.get("version") != null) ? params.get("version").get(0) : null,
//                (params.get("species") != null) ? params.get("species").get(0) : null,
//                endTime);

        try {
            return buildResponse(Response.ok(jsonObjectWriter.writeValueAsString(obj), MediaType.APPLICATION_JSON_TYPE));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            logger.error("Error parsing queryResponse object");

            return buildResponse(Response.ok("Error parsing queryResponse JSON object"));
        }
    }

    protected Response createOkResponse(Object o1, MediaType o2) {
        return buildResponse(Response.ok(o1, o2));
    }

    protected Response createOkResponse(Object o1, MediaType o2, String fileName) {
        return buildResponse(Response.ok(o1, o2).header("content-disposition", "attachment; filename =" + fileName));
    }

    private Response buildResponse(ResponseBuilder responseBuilder) {
        return responseBuilder.header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Headers", "x-requested-with, content-type").build();
    }

    private String encryptResponse(Object obj){
        String encryptedMessage = "";

        try {
            String jsonStr = jsonObjectWriter.writeValueAsString(obj);

            Properties prop = new Properties();
            prop.load(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("aes.properties") ));
            String keyStoreFileLocation = getClass().getClassLoader().getResource("aes-keystore.jck").getFile();
            String storePass = prop.get("storepass").toString();
            String alias = prop.get("alias").toString();
            String keyPass = prop.get("keypass").toString();
            String iv = prop.get("IV").toString();

            Key keyFromKeyStore = KeystoreUtil.getKeyFromKeyStore(keyStoreFileLocation, storePass, alias, keyPass);
            AESCipher cipherWithIv = new AESCipher(keyFromKeyStore, iv.getBytes());
            encryptedMessage = cipherWithIv.getEncryptedMessage(jsonStr);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return encryptedMessage;
    }
}

