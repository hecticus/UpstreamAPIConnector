package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import exceptions.UpstreamException;
import models.Config;
import models.clients.Client;
import models.clients.ClientHasDevices;
import org.apache.commons.codec.binary.Base64;
import play.Logger;
import play.libs.F;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSRequestHolder;
import play.libs.ws.WSResponse;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import utils.UpstreamCoreUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by plessmann on 05/06/15.
 */
public class Upstream extends UpstreamController {

    private static final String UPSTREAM_STATUS_URL = "/game/user/status";
    private static final String UPSTREAM_PASSWORD_URL = "/game/user/password";
    private static final String UPSTREAM_EVENT_URL = "/game/user/event";
    private static final String UPSTREAM_LOGIN_URL = "/game/user/login";
    private static final String UPSTREAM_SUBSCRIBE_URL = "/game/user/subscribe";

    //private static final String upstreamUserIDSubscriptionResponseTag = "user_id"; //segun documentacion
    private static final String upstreamUserIDSubscriptionResponseTag = "userId"; //segun pruebas

    //Reset Upstream pass and they send MT to client with new one
    public static Result resetUpstreamPass() {
        String msisdn = "";
        try{
            ObjectNode response = null;
            ObjectNode clientData = getJson();
            Client client = null;
            //Obtenemos el canal por donde esta llegando el request
            String upstreamChannel;
            if(clientData.has("upstreamChannel")){
                upstreamChannel = clientData.get("upstreamChannel").asText();
            }else{
                upstreamChannel = "Android"; //"Android" o "Web"
            }
            //buscamos el msisdn
            if(clientData.has("msisdn")){
                msisdn = clientData.get("msisdn").asText();
                client = Client.getByLogin(msisdn);
            }
            if(client != null) {
                resetPasswordForUpstream(client,upstreamChannel);
                client.setPassword("");
                client.update();
                response = buildBasicResponse(0, "OK", client.toJson());
            } else {
                response = buildBasicResponse(2, "no existe el registro para hacer reset del pass");
            }
            return ok(response);
        } catch (Exception ex) {
            UpstreamCoreUtils.printToLog(Upstream.class, "Error manejando clients", "error recuperando el password de upstream del client " + msisdn, true, ex, "support-level-1", Config.LOGGER_ERROR);
            return Results.badRequest(buildBasicResponse(3, "ocurrio un error recuperando password", ex));
        }
    }

    //Events for Upstream
//    public static Result sendEvent() {
//        String user_id = "";
//        String event_type = "";
//        try{
//            ObjectNode response = null;
//            ObjectNode clientData = getJson();
//            Client client = null;
//            ObjectNode metadata = null;
//            //Obtenemos el canal por donde esta llegando el request
//            String upstreamChannel;
//            if(clientData.has("upstreamChannel")){
//                upstreamChannel = clientData.get("upstreamChannel").asText();
//            }else{
//                upstreamChannel = "Android"; //"Android" o "Web"
//            }
//            //buscamos el user_id
//            if(clientData.has("user_id")){
//                user_id = clientData.get("user_id").asText();
//                client = Client.finder.where().eq("user_id",user_id).findUnique();
//            }
//            //buscamos el event_type
//            if(clientData.has("event_type")){
//                event_type = clientData.get("event_type").asText();
//            }
//
//            if(clientData.has("metadata")){
//                metadata = (ObjectNode) clientData.get("metadata");
//            }
//
//            if(client != null && (event_type != null || !event_type.isEmpty())) {
//                sendEventForUpstream(client,upstreamChannel,event_type, metadata);
//                response = buildBasicResponse(0, "OK", client.toJson());
//            } else {
//                response = buildBasicResponse(2, "no existe el registro para enviar el evento");
//            }
//            return ok(response);
//        } catch (Exception ex) {
//            UpstreamCoreUtils.printToLog(Upstream.class, "Error enviando evento", "error al enviar un evento a Upstream del client " + user_id, true, ex, "support-level-1", Config.LOGGER_ERROR);
//            return Results.badRequest(buildBasicResponse(3, "ocurrio un error enviando evento", ex));
//        }
//    }

    public static Result sendClientEvent(Integer id) {
        String user_id = null;
        String event_type = null;
        try{
            ObjectNode clientData = getJson();
            if(clientData == null){
                return badRequest(buildBasicResponse(1, "Falta el json con los parametros del request"));
            }
            Client client = Client.getByID(id);
            if(client != null) {
                user_id = client.getUserId();
                ObjectNode metadata = null;
                //Obtenemos el canal por donde esta llegando el request
                String upstreamChannel;
                if (clientData.has("upstreamChannel")) {
                    upstreamChannel = clientData.get("upstreamChannel").asText();
                } else {
                    upstreamChannel = "Android"; //"Android" o "Web"
                }
                //buscamos el event_type
                if (clientData.has("event_type")) {
                    event_type = clientData.get("event_type").asText();
                }

                if (clientData.has("metadata")) {
                    metadata = (ObjectNode) clientData.get("metadata");
                }

                if (event_type != null || !event_type.isEmpty()) {
                    sendEventForUpstream(client, upstreamChannel, event_type, metadata);
                    return ok(buildBasicResponse(0, "OK", client.toJson()));
                } else {
                    return badRequest(buildBasicResponse(3, "Falta el tipo de evento"));
                }
            } else {
                return notFound(buildBasicResponse(2, "no existe el cliente " + id));
            }
        } catch (Exception ex) {
            ObjectNode response;
            if(ex instanceof UpstreamException){
                UpstreamException upstreamException = (UpstreamException) ex;
                UpstreamCoreUtils.printToLog(Upstream.class, "Error manejando clients", "error al enviar un evento a Upstream del client " + user_id + " el request fue " + upstreamException.getRequest(), true, ex, "support-level-1", Config.LOGGER_ERROR);
                response = buildUpstreamResponse(-2, "ocurrio un error creando el registro", upstreamException);
            } else {
                UpstreamCoreUtils.printToLog(Upstream.class, "Error manejando clients", "error al enviar un evento a Upstream del client " + user_id, true, ex, "support-level-1", Config.LOGGER_ERROR);
                response = buildBasicResponse(-1, "ocurrio un error creando el registro", ex);
            }
            return internalServerError(response);
        }
    }

    //FUNCIONES DE UPSTREAM

    /**
     * Funcion que permite suscribir al usuario en Upstream dado su msisdn(username)
     *
     * POST data as JSON:
     * msisdn                   String  mandatory   msisdn from user (login)
     * push_notification_id     String  optional    push id
     * password                 String  mandatory   password sent from SMS to the user
     * service_id               String  mandatory   Upstream suscription service
     * metadata                 JSON    optional    extra params
     *
     * OUTPUT JSON FROM UPSTREAM:
     * result       int     0-Success, 1-User already subscribed, 2-User cannot be identified, 3-User not Subscribed, 5-Invalid MSISDN, 6-push_notification_id already exists, 7-Upstream service no longer available
     * user_id      String
     *
     * Example:
     *
     * Headers:
     * Content-Type : application/json
     * Accept : application/gamingapi.v1+json
     * x-gameapi-app-key : DEcxvzx98533fdsagdsfiou
     * Body:
     * {"password":"CMSLJMWD","metadata":{"channel":"Android","result":null,"points":null,
     * "app_version":"gamingapi.v1","session_id":null},"service_id":"prototype-app -SubscriptionDefault",
     * "msisdn":"999000000005","push_notification_id":"wreuoi24lkjfdlkshjkjq4h35k13jh43kjhfkjqewhrtkqjrewht"}
     *
     * Response:
     * {
     * "result" : 0
     * "user_id" : "324234345050505"
     * }
     *
     *
     * Parametros necesarios
     * username  user from the app
     * password  password from the app
     * push_notification_id  optional, regID of user
     * channel   "Android" or "Web"
     *
     */
    public static void subscribeUserToUpstream(Client client, String upstreamChannel, String operation) throws UpstreamException{
        String upstreamGuestUser = Config.getString("upstreamGuestUser");
        if(client.getLogin() == null || client.getLogin().equalsIgnoreCase(upstreamGuestUser)){
            if(client.getLogin() == null){
                client.setLogin(upstreamGuestUser);
            }
            if(client.getPassword() == null){
                client.setPassword(Config.getString("upstreamGuestPassword"));
            }
            if(client.getUserId() == null){
                client.setUserId(Config.getString("upstreamUserID"));
            }
            client.setStatus(2);
        } else {
            String msisdn = client.getLogin();
            String password = client.getPassword();
            String push_notification_id = null;

            push_notification_id = getPushNotificationID(client, upstreamChannel);

            //Data from configs
            String upstreamURL = Config.getString("upstreamURL");
            String url = upstreamURL+UPSTREAM_SUBSCRIBE_URL;

            WSRequestHolder urlCall = setUpstreamRequest(url, msisdn, password);

            //llenamos el JSON a enviar
            ObjectNode fields = getBasicUpstreamPOSTRequestJSON(upstreamChannel, push_notification_id, null, client.getSession());
            //agregamos el msisdn(username) y el password
            fields.put("password", password);
            fields.put("msisdn", msisdn);
            fields.put("username", msisdn);

            //audit log for points
            upstreamRequestLoggersubscribe(msisdn, fields, operation, url);


            //realizamos la llamada al WS
            F.Promise<play.libs.ws.WSResponse> resultWS = urlCall.post(fields);
            WSResponse wsResponse = resultWS.get(Config.getLong("ws-timeout-millis"), TimeUnit.MILLISECONDS);

            //audit log for responses


            checkUpstreamResponseStatus(wsResponse,client, fields.toString());
            ObjectNode fResponse = Json.newObject();
            fResponse = (ObjectNode)wsResponse.asJson();
            String errorMessage="";
            upstreamResponseLoggersubscribe(msisdn, wsResponse, fResponse, operation);
            if(fResponse != null){
                int callResult = fResponse.findValue("result").asInt();
                errorMessage = getUpstreamError(callResult) + " - upstreamResult:"+callResult;
                //TODO: revisar si todos estos casos devuelven con exito la llamada o algunos si se consideran errores
                if(callResult == 0 || callResult == 1 || callResult == 0 || callResult == 6){
                    //Se trajo la informacion con exito
                    String userID = fResponse.findValue(upstreamUserIDSubscriptionResponseTag).asText();
                    //TODO: guardar el userID en la info del cliente
                    client.setUserId(userID);
                }else{
                    //ocurrio un error en la llamada
                    throw new UpstreamException(callResult, errorMessage, fields.toString());
                }
            }else{
                errorMessage = "Web service call to Upstream failed";
                throw new UpstreamException(-1, errorMessage, fields.toString());
            }

            //client.setStatus(1);//no lo debemos colocar en status 1 hasta que no obtengamos el status del cliente
        }
    }

    /**
     * Funcion que permite desuscribir al usuario en Upstream dado su user_id de upstream
     *
     * POST data as JSON:
     * user_id                  String  mandatory   upstream user_id
     * push_notification_id     String  optional    push id
     * service_id               String  mandatory   Upstream suscription service
     * metadata                 JSON    optional    extra params
     *
     * OUTPUT JSON FROM UPSTREAM:
     * result       int     0-Success, 2-User cannot be identified, 3-User not Subscribed, 4-push_notification_id missing, 7-Upstream service no longer available
     *
     * Example:
     *
     * Headers:
     * Content-Type : application/json
     * Accept : application/gamingapi.v1+json
     * x-gameapi-app-key : DEcxvzx98533fdsagdsfiou
     * Body:
     * {"metadata":{"channel":"Android","result":null,"points":null,
     * "app_version":"gamingapi.v1","session_id":null},"service_id":"prototype-app -SubscriptionDefault",
     * "user_id":8001,"push_notification_id":"wreuoi24lkjfdlkshjkjq4h35k13jh43kjhfkjqewhrtkqjrewht"}
     *
     * Response:
     * {
     * "result" : 0
     * }
     *
     *
     * Parametros necesarios
     * user_id   upstream user_id
     * username  user from the app
     * password  password from the app
     * push_notification_id  optional, regID of user
     * channel   "Android" or "Web"
     *
     */
    public static void unsubscribeUserToUpstream(Client client, String upstreamChannel) throws Exception{
        String errorMessage="";
        if(client.getLogin() == null || client.getUserId() == null){
            errorMessage = "El cliente no posee login o user_id";
            throw new Exception(errorMessage);
        } else {
            String login = client.getLogin();
            String userID = client.getUserId();
            String password = client.getPassword();
            String push_notification_id = null;
            push_notification_id = getPushNotificationID(client, upstreamChannel);

            //Data from configs
            String upstreamURL = Config.getString("upstreamURL");
            String url = upstreamURL + UPSTREAM_SUBSCRIBE_URL;

            WSRequestHolder urlCall = setUpstreamRequest(url, login, password);

            //llenamos el JSON a enviar
            ObjectNode fields = getBasicUpstreamPOSTRequestJSON(upstreamChannel, push_notification_id, null, client.getSession());
            //agregamos el user_id
            fields.put("user_id", userID);

            //realizamos la llamada al WS
            F.Promise<play.libs.ws.WSResponse> resultWS = urlCall.post(fields);
            WSResponse wsResponse = resultWS.get(Config.getLong("ws-timeout-millis"), TimeUnit.MILLISECONDS);
            checkUpstreamResponseStatus(wsResponse,client, fields.toString());
            ObjectNode fResponse = Json.newObject();
            fResponse = (ObjectNode)wsResponse.asJson();
            if(fResponse != null){
                int callResult = fResponse.findValue("result").asInt();
                errorMessage = getUpstreamError(callResult) + " - upstreamResult:"+callResult;
                if(callResult == 0){
                    //TODO: Que se debe hacer en el caso que la desuscripcion sea exitosa, borrar al cliente, ponerlo en status 2 para que con la fecha se actualice?
                    client.setStatus(0);
                    client.setUserId("");
                    client.setPassword("");
                }else{
                    //ocurrio un error en la llamada
                    throw new UpstreamException(callResult, errorMessage, fields.toString());
                }
            }else{
                errorMessage = "Web service call to Upstream failed";
                throw new UpstreamException(-1, errorMessage, fields.toString());
            }
        }
    }

    /**
     * Funcion que permite hacer login del usuario en Upstream dado un username y un password
     *
     * POST data as JSON:
     * username                 String  mandatory   user name
     * push_notification_id     String  optional    push id
     * password                 String  mandatory   password sent from SMS to the user
     * service_id               String  mandatory   Upstream suscription service
     * metadata                 JSON    optional    extra params
     *
     * OUTPUT JSON FROM UPSTREAM:
     * result       int     0-Success, 2-User cannot be identified, 3-User not Subscribed, 6-push_notification_id already exists, 7-Upstream service no longer available
     * user_id      String
     *
     * Example:
     *
     * Headers:
     * Content-Type : application/json
     * Accept : application/gamingapi.v1+json
     * x-gameapi-app-key : DEcxvzx98533fdsagdsfiou
     * Body:
     * {"password":"CMSLJMWD","metadata":{"channel":"Android","result":null,"points":null,
     * "app_version":"gamingapi.v1","session_id":null},"service_id":"prototype-app -SubscriptionDefault",
     * "username":"999000000005","push_notification_id":"wreuoi24lkjfdlkshjkjq4h35k13jh43kjhfkjqewhrtkqjrewht"}
     *
     * Response:
     * {
     * "result" : 0
     * "user_id" : "324234345050505"
     * }
     *
     * Parameters required
     * username  user from the app
     * password  password from the app
     * push_notification_id  optional, regID of user
     * channel   "Android" or "Web"
     *
     */
    public static void getUserIdFromUpstream(Client client, String upstreamChannel) throws UpstreamException{
        String upstreamGuestUser = Config.getString("upstreamGuestUser");
        if(client.getLogin() == null || client.getLogin().equalsIgnoreCase(upstreamGuestUser)){
            if(client.getLogin() == null){
                client.setLogin(upstreamGuestUser);
            }
            if(client.getPassword() == null){
                client.setPassword(Config.getString("upstreamGuestPassword"));
            }
            if(client.getUserId() == null){
                client.setUserId(Config.getString("upstreamUserID"));
            }
            client.setStatus(2);
        } else {
            String username = client.getLogin();
            String password = client.getPassword();
            String push_notification_id = getPushNotificationID(client, upstreamChannel);
            //Data from configs
            String upstreamURL = Config.getString("upstreamURL");
            String url = upstreamURL + UPSTREAM_LOGIN_URL;

            WSRequestHolder urlCall = setUpstreamRequest(url, username, password);

            //llenamos el JSON a enviar
            ObjectNode fields = getBasicUpstreamPOSTRequestJSON(upstreamChannel, push_notification_id, null, client.getSession());
            //agregamos el username y el password
            fields.put("password", password);
            fields.put("username", username);
            fields.put("msisdn", username);
//            printRequest(urlCall, fields);
            //realizamos la llamada al WS
            F.Promise<play.libs.ws.WSResponse> resultWS = urlCall.post(fields);
            WSResponse wsResponse = resultWS.get(Config.getLong("ws-timeout-millis"), TimeUnit.MILLISECONDS);

            String upstreamGuestUserId = Config.getString("upstreamUserID");
            if(client.getUserId() != null && client.getUserId().equalsIgnoreCase(upstreamGuestUserId)){
                return;
            }

            checkUpstreamResponseStatus(wsResponse,client, fields.toString());
            ObjectNode fResponse = Json.newObject();
            fResponse = (ObjectNode)wsResponse.asJson();
            String errorMessage="";
            if(fResponse != null){
                int callResult = fResponse.findValue("result").asInt();
                errorMessage = getUpstreamError(callResult) + " - upstreamResult:"+callResult;
                if(callResult == 0 || callResult == 6){
                    //Se trajo la informacion con exito
                    String userID = fResponse.findValue("user_id").asText();
                    //TODO: guardar el userID en la info del cliente
                    client.setUserId(userID);
                }else{
                    //ocurrio un error en la llamada
                    throw new UpstreamException(callResult, errorMessage, fields.toString());
                }
            }else{
                errorMessage = "Web service call to Upstream failed";
                throw new UpstreamException(-1, errorMessage, fields.toString());
            }
        }
    }

    /**
     * Funcion que permite obtener el status de una suscripcion en Upstream
     *
     * POST data as JSON:
     * user_id                  String  mandatory   upstream user_id
     * push_notification_id     String  optional    push id
     * service_id               String  mandatory   Upstream suscription service
     * metadata                 JSON    optional    extra params
     *
     * OUTPUT JSON FROM UPSTREAM:
     * result       int     0-Success, 2-User cannot be identified, 3-User not Subscribed, 4-push_notification_id missing, 7-Upstream service no longer available
     * user_id      String
     *
     * Example:
     *
     * Headers:
     * Content-Type : application/json
     * Accept : application/gamingapi.v1+json
     * x-gameapi-app-key : DEcxvzx98533fdsagdsfiou
     * Authorization : Basic OTk5MDAwMDIzMzE1OlNSUTcyRktT
     * Body:
     * {"metadata":{"channel":"Android","result":null,"points":null,"app_version":"gamingapi.v1",
     * "session_id":null},"service_id":"prototype-app -SubscriptionDefault",
     * "user_id":8001,"push_notification_id":"wreuoi24lkjfdlk13jh45kjhfkjqewhrt34jrewh2"}
     *
     * Response:
     * {
     * "result" : 0,
     * "eligible" : "true",
     * "credits_left" : "10"
     * }
     *
     * Parametros necesarios
     * userID    upstream user id
     * username  user from the app
     * password  password from the app
     * push_notification_id  optional, regID of user
     * channel   "Android" or "Web"
     *
     */
    public static void getStatusFromUpstream(Client client, String upstreamChannel) throws UpstreamException{
        String upstreamGuestUser = Config.getString("upstreamGuestUser");
        if(client.getLogin() != null && client.getUserId() != null && client.getPassword() != null && !client.getLogin().equalsIgnoreCase(upstreamGuestUser)){
            String username = client.getLogin();
            String userID = client.getUserId();
            String password = client.getPassword();
            String push_notification_id = getPushNotificationID(client, upstreamChannel);

            //Data from configs
            String upstreamURL = Config.getString("upstreamURL");
            String url = upstreamURL + UPSTREAM_STATUS_URL;

            //Hacemos la llamada con los headers de autenticacion
            WSRequestHolder urlCall = setUpstreamRequest(url, username, password);

            //llenamos el JSON a enviar
            ObjectNode fields = getBasicUpstreamPOSTRequestJSON(upstreamChannel, push_notification_id, null, client.getSession());
            fields.put("user_id", userID); //agregamos el UserID al request

            upstreamRequestLoggersubscribe(username, fields, "status", url);

            //realizamos la llamada al WS
            F.Promise<play.libs.ws.WSResponse> resultWS = urlCall.post(fields);
            WSResponse wsResponse = resultWS.get(Config.getLong("ws-timeout-millis"), TimeUnit.MILLISECONDS);

            String upstreamGuestUserId = Config.getString("upstreamUserID");
            if(client.getUserId() != null && client.getUserId().equalsIgnoreCase(upstreamGuestUserId)){
                return;
            }

            checkUpstreamResponseStatus(wsResponse,client, fields.toString());
            ObjectNode fResponse = Json.newObject();
            fResponse = (ObjectNode)wsResponse.asJson();
            String errorMessage = "";
            upstreamResponseLoggersubscribe(username, wsResponse, fResponse, "status");
            if(fResponse != null){
                int callResult = fResponse.findValue("result").asInt();
                errorMessage = getUpstreamError(callResult) + " - upstreamResult:"+callResult;
                if(callResult == 0 || callResult == 4){
                    //Se trajo la informacion con exito
                    Boolean eligible = fResponse.findValue("eligible").asBoolean();
                    //TODO: guardar en el userID la info de si esta activo o no
                    client.setStatus(eligible ? 1 : -1);
                }else{
                    //ocurrio un error en la llamada
                    throw new UpstreamException(callResult, errorMessage, fields.toString());
                }
            }else{
                errorMessage = "Web service call to Upstream failed";
                throw new UpstreamException(-1, errorMessage, fields.toString());
            }
        }else{
            //deberia estar en periodo de pruebas 2 o desactivado por tiempo -1 la verificacion se hace despues de la llamada
            client.setStatus(2);
        }
    }

    /**
     * Funcion que permite que Upstream envie un MT con el password nuevamente al cliente
     *
     * POST data as JSON:
     * msisdn                   String  mandatory   upstream user_id
     * push_notification_id     String  optional    push id
     * service_id               String  mandatory   Upstream suscription service
     * metadata                 JSON    optional    extra params
     *
     * OUTPUT JSON FROM UPSTREAM:
     * result       int     0-Success, 2-User cannot be identified, 3-User not Subscribed, 7-Upstream service no longer available
     *
     * Example:
     *
     * Headers:
     * Content-Type : application/json
     * Accept : application/gamingapi.v1+json
     * x-gameapi-app-key : DEcxvzx98533fdsagdsfiou
     * Authorization : Basic OTk5MDAwMDIzMzE1OlNSUTcyRktT
     * Body:
     * {"metadata":{"channel":"Android","result":null,"points":null,"app_version":"gamingapi.v1",
     * "session_id":null},"service_id":"prototype-app -SubscriptionDefault",
     * "msisdn":"999000000005"}
     *
     * Response:
     * {
     * "result" : 0
     * }
     *
     * msisdn    msisdn for upstream client
     * push_notification_id  optional, regID of user
     * channel   "Android" or "Web"
     *
     */
    public static void resetPasswordForUpstream(Client client, String upstreamChannel) throws Exception{
        String errorMessage = "";
        if(client.getLogin() != null){
            String msisdn = client.getLogin();
            String userID = null;
            String password = null;
            String push_notification_id = getPushNotificationID(client, upstreamChannel);

            //Data from configs
            String upstreamURL = Config.getString("upstreamURL");
            String url = upstreamURL + UPSTREAM_PASSWORD_URL;

            //Hacemos la llamada con los headers de autenticacion
            WSRequestHolder urlCall = setUpstreamRequest(url, msisdn, password);

            //llenamos el JSON a enviar
            ObjectNode fields = getBasicUpstreamPOSTRequestJSON(upstreamChannel, push_notification_id, null, client.getSession());
            fields.put("msisdn", msisdn); //agregamos el UserID al request

            upstreamRequestLoggersubscribe(msisdn, fields, "reset", url);

            //realizamos la llamada al WS
            F.Promise<play.libs.ws.WSResponse> resultWS = urlCall.post(fields);
            WSResponse wsResponse = resultWS.get(Config.getLong("ws-timeout-millis"), TimeUnit.MILLISECONDS);
            checkUpstreamResponseStatus(wsResponse,client, fields.toString());
            ObjectNode fResponse = Json.newObject();
            fResponse = (ObjectNode)wsResponse.asJson();
            upstreamResponseLoggersubscribe(msisdn, wsResponse, fResponse, "reset");
            if(fResponse != null){
                int callResult = fResponse.findValue("result").asInt();
                errorMessage = getUpstreamError(callResult) + " - upstreamResult:"+callResult;
                if(callResult == 0){
                    //everything is OK, do nothing but wait for MT
                }else{
                    //ocurrio un error en la llamada
                    throw new UpstreamException(callResult, errorMessage, fields.toString());
                }
            }else{
                errorMessage = "Web service call to Upstream failed";
                throw new UpstreamException(-1, errorMessage, fields.toString());
            }
        }else{
            errorMessage = "No MSISDN for client";
            throw new Exception(errorMessage);
        }
    }

    /**
     * Funcion que permite enviar un evento de la app a Upstream
     *
     * POST data as JSON:
     * user_id                  String  mandatory   upstream user_id
     * push_notification_id     String  optional    push id
     * service_id               String  mandatory   Upstream suscription service
     * metadata                 JSON    optional    extra params
     * timestamp                String  mandatory   “dd/MM/yy HH:mm:ss.SSS UTC” (TZ is always UTC)
     * device_id                String  mandatory   device ID for upstream
     * event_type               String  optional    String with one of the following
     *
     * EVENTS
     *
     * APP_LAUNCH: Application Launch
     * LOGIN: Login attempt
     * GAME_LAUNCH: Game Launch
     * GAME_END: Game End
     * APP_CLOSE: Application Close
     * UPD_POINTS: Points Update
     * VIEW_SP: View subscription prompt
     * CLICK_SP: Clicked subscription prompt
     * CLICK_PN: Clicked Push Notification
     *
     * OUTPUT JSON FROM UPSTREAM:
     * result       int     0-Success, 2-User cannot be identified, 3-User not Subscribed, 4-push_notification_id missing, 7-Upstream service no longer available
     *
     * Example:
     *
     * Headers:
     * Content-Type: application/json
     * Accept: application/gamingapi.v1+json
     * x-gameapi-app-key: DEcxvzx98533fdsagdsfiou
     * Authorization : Basic OTk5MDAwMDIzMzE1OlNSUTcyRktT
     *
     * Body:
     * {"timestamp":"01/01/14 00:00:01.001
     * UTC","metadata":{"channel":"Android","result":"win","points":[{"type":"expe
     * rience","value":"100"}],"app_version":"gamingapi.v1","session_id":null},"se
     * rvice_id":"prototype-app -
     * SubscriptionDefault","user_id":8001,"push_notification_id":"wreuoi24lkjfdlk
     * 13jh45kjhfkjqewhrt34jrewh2","event_type":"APP_LAUNCH", "device_id":"user-device-id"}
     *
     * Response:
     * {
     * "result" : 0
     * }
     *
     * Parametros necesarios
     * userID    upstream user id
     * username  user from the app
     * password  password from the app
     * push_notification_id  optional, regID of user
     * channel   "Android" or "Web"
     *
     */
    public static void sendEventForUpstream(Client client, String upstreamChannel, String event_type, ObjectNode metadata) throws Exception{
        if(client.getLogin() != null && client.getUserId() != null && client.getPassword() != null){
            String username = client.getLogin();
            String userID = client.getUserId();
            String password = client.getPassword();
            String push_notification_id = getPushNotificationID(client, upstreamChannel);

            //Data from configs
            String upstreamURL = Config.getString("upstreamURL");
            String url = upstreamURL + UPSTREAM_EVENT_URL;

            //Hacemos la llamada con los headers de autenticacion
            WSRequestHolder urlCall = setUpstreamRequest(url, username, password);

            //llenamos el JSON a enviar
            ObjectNode fields = getBasicUpstreamPOSTRequestJSON(upstreamChannel, push_notification_id, metadata, client.getSession());
            fields.put("user_id", userID); //agregamos el UserID al request
            fields.put("event_type", event_type); //agregamos el evento
            fields.put("timestamp", formatDateUpstream()); //agregamos el time

            //audit log for points
            upstreamRequestLogger(client, fields, event_type);

            //realizamos la llamada al WS
            F.Promise<play.libs.ws.WSResponse> resultWS = urlCall.post(fields);
            WSResponse wsResponse = resultWS.get(Config.getLong("ws-timeout-millis"), TimeUnit.MILLISECONDS);

            //audit log for responses
            upstreamResponseLogger(client, wsResponse, event_type);
            String upstreamGuestUserId = Config.getString("upstreamUserID");
            if(client.getUserId() != null && client.getUserId().equalsIgnoreCase(upstreamGuestUserId)){
                return;
            }

            checkUpstreamResponseStatus(wsResponse, client, fields.toString());
            ObjectNode fResponse = Json.newObject();
            fResponse = (ObjectNode)wsResponse.asJson();
            String errorMessage = "";
            if(fResponse != null){
                int callResult = fResponse.findValue("result").asInt();
                errorMessage = getUpstreamError(callResult) + " - upstreamResult:"+callResult;
                if(callResult == 0 || callResult == 4){
                    if(fResponse.has("eligible")) {
                        //Se trajo la informacion con exito
                        Boolean eligible = fResponse.findValue("eligible").asBoolean();
                        //TODO: guardar en el userID la info de si esta activo o no
                        client.setStatus(eligible ? 1 : 0);
                    }
                }else{
                    //ocurrio un error en la llamada
                    throw new UpstreamException(callResult, errorMessage, fields.toString());
                }
            }else{
                errorMessage = "Web service call to Upstream failed";
                throw new UpstreamException(-1, errorMessage, fields.toString());
            }
        }else{
            //deberia estar en periodo de pruebas 2 o desactivado por tiempo -1 la verificacion se hace despues de la llamada
            client.setStatus(2);
        }
    }

    //UPSTREAM COMMONS
    //set headers and url call
    private static WSRequestHolder setUpstreamRequest(String url, String username, String password){
        String upstreamAppVersion = Config.getString("upstreamAppVersion"); //gamingapi.v1
        String upstreamAppKey = Config.getString("upstreamAppKey"); //DEcxvzx98533fdsagdsfiou

        String authString = null;
        if(username != null && !username.isEmpty() && password != null && !password.isEmpty()){
            authString = username+":"+password.toUpperCase();
            byte[] encodedBytes = Base64.encodeBase64(authString.getBytes());
            authString = new String(encodedBytes);
        }

        //Hacemos la llamada con los headers de autenticacion
        WSRequestHolder urlCall = WS.url(url).setContentType("application/json");
        //FORMAT:  "Authentication: username:password" //BASE64
        //urlCall.setHeader("Authorization","Basic AW4rRRcpbjpvcGVuIHNlc2FtZQ==");
        if(authString != null) urlCall.setHeader("Authorization","Basic "+authString);
        urlCall.setHeader("x-gameapi-app-key",upstreamAppKey);

        //The different versions of the API are defined in the HTTPS Accept header.
        urlCall.setHeader("Accept","application/"+upstreamAppVersion+"+json");
        urlCall.setMethod("POST");
        return urlCall;
    }

    //set basic POST data for UPSTREAM
    private static ObjectNode getBasicUpstreamPOSTRequestJSON(String upstreamChannel, String push_notification_id, ObjectNode metadata, String sessionId){
        String upstreamServiceID = Config.getString("upstreamServiceID"); //prototype-app -SubscriptionDefault
        String upstreamAppVersion = Config.getString("upstreamAppVersion"); //gamingapi.v1

        ObjectNode fields = Json.newObject();
        if(metadata == null) {
            metadata = Json.newObject();
        }
        fields.put("service_id", upstreamServiceID);
        if(push_notification_id != null && !push_notification_id.isEmpty()){// && upstreamChannel.equalsIgnoreCase("Android")){
            fields.put("push_notification_id",push_notification_id);
            fields.put("device_id",push_notification_id);
        }
        //"channel":"Android","result":null,"points":null, "app_version":"gamingapi.v1","session_id":null
        metadata.put("channel",upstreamChannel);
        metadata.put("app_version",upstreamAppVersion);
        if(sessionId != null) {
            metadata.put("session_id", sessionId);
        }
        fields.put("metadata",metadata);
        return fields;
    }

    //get push_notification_id for upstream
    private static String getPushNotificationID(Client client, String channel){
        String push_notification_id = null;
        try {
            if(client.getUserId() != null && client.getUserId().equalsIgnoreCase(Config.getString("upstreamUserID"))) {
                push_notification_id = Config.getString("upstreamGuestDeviceId");
            }else{
                List<ClientHasDevices> devices = client.getDevices();
                for (int i = 0; i < devices.size(); i++) {
                    if (devices.get(i).getDevice().getName().equalsIgnoreCase(channel)) {
                        //con el primer push_notification_id nos basta por ahora
                        push_notification_id = devices.get(i).getRegistrationId();
                        break;
                    }
                }
                if(push_notification_id == null){
                    push_notification_id = UUID.randomUUID().toString();
                }
            }
        } catch (Exception e) {
            //no hacemos nada si esto falla
        }
        return push_notification_id;
    }

    //0-Success, 2-User cannot be identified, 3-User not Subscribed, 4-push_notification_id missing, 6-push_notification_id already exists, 7-Upstream service no longer available
    private static String getUpstreamError(int errorCode){
        switch (errorCode){
            case 0: return "Success";
            case 1: return "User already subscribed";
            case 2: return "User cannot be identified";
            case 3: return "User not Subscribed";
            case 4: return "Push_notification_id missing";
            case 5: return "Invalid MSISDN";
            case 6: return "Push_notification_id already exists";
            case 7: return "Upstream service no longer available";
            default: return "Error not recognized";
        }
    }

    //check response status
    private static void checkUpstreamResponseStatus(WSResponse wsResponse, Client client, String request) throws UpstreamException {
        int wsStatus = wsResponse.getStatus();
        if(wsStatus == 200){
            //all OK
        }else{
            if(wsStatus == 400 || wsStatus == 403 || wsStatus == 404 || wsStatus == 500 || wsStatus == 503){
                throw new UpstreamException(wsStatus, "Upstream service: "+ wsResponse.getUri() +" fails with status: "+wsStatus, request);
            }else{
                if(wsStatus == 401){
                    //la combinacion login:password es incorrecta, borramos el password
                    client.setPassword("");
                    throw new UpstreamException(wsStatus, "Upstream service: "+ wsResponse.getUri() +" fails authentication", request);
                }else{
                    throw new UpstreamException(wsStatus, "Upstream service: "+ wsResponse.getUri() +" fails with unknown status", request);
                }
            }
        }
    }

    //“dd/MM/yy HH:mm:ss.SSS UTC” (TZ is always UTC)
    private static String formatDateUpstream() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm:ss.SSS");
        String date = sdf.format(new Date());
        date = date+" UTC";
        return date;
    }

    //FAKE UPSTREAM RESPONSE
    public static Result upstreamFakeCreate() {
        Http.Request req = Http.Context.current().request();
        Map<String, String[]> headerMap = req.headers();
        boolean hasAuth = false;
        for (String headerKey : headerMap.keySet()) {
            if(headerKey.equals("Authorization")){
                hasAuth = true;
            }
            //System.out.println("Key: " + headerKey + " - Value: " + headerMap.get(headerKey)[0]);
        }
        ObjectNode response = Json.newObject();
        response.put("result",0);
        response.put(upstreamUserIDSubscriptionResponseTag,"324234345050505");
        return ok(response);
    }
    public static Result upstreamFakeLogin() {
        Http.Request req = Http.Context.current().request();
        Map<String, String[]> headerMap = req.headers();
        boolean hasAuth = false;
        for (String headerKey : headerMap.keySet()) {
            if(headerKey.equals("Authorization")){
                hasAuth = true;
            }
            //System.out.println("Key: " + headerKey + " - Value: " + headerMap.get(headerKey)[0]);
        }
        ObjectNode response = Json.newObject();
        if(!hasAuth){
            response.put("result",2);
        }else{
            response.put("result",0);
        }
        response.put("user_id","324234345050505");
        return ok(response);
    }

    public static Result upstreamFakeStatus() {
        Http.Request req = Http.Context.current().request();
        Map<String, String[]> headerMap = req.headers();
        boolean hasAuth = false;
        for (String headerKey : headerMap.keySet()) {
            if(headerKey.equals("Authorization")){
                hasAuth = true;
            }
            //System.out.println("Key: " + headerKey + " - Value: " + headerMap.get(headerKey)[0]);
        }
        ObjectNode response = Json.newObject();
        if(!hasAuth){
            response.put("eligible",false);
        }else{
            response.put("eligible",true);
        }
        response.put("result",0);

        response.put("credits_left",10);
        return ok(response);
    }

    public static Result upstreamFakeResetPass() {
        ObjectNode response = Json.newObject();
        response.put("result",0);
        return ok(response);
    }

    public static Result upstreamFakeEventSend() {
        ObjectNode response = Json.newObject();
        response.put("result",0);
        return ok(response);
    }

    private static void upstreamRequestLogger(Client client, ObjectNode metadata, String eventType) {
        try {
            if (eventType.equalsIgnoreCase("UPD_POINTS")){
                //log event
                Logger.of("upstream").trace("id_client:" + client.getIdClient() + " user_id:" + client.getUserId() + " metadata: "+metadata.toString());
            }//else skip
        }catch (Exception ex){
            //do nothing catch to avoid interruptions
        }
    }

    private static void upstreamResponseLogger(Client client, WSResponse wsResponse, String eventType){
        try {
            if (eventType.equalsIgnoreCase("UPD_POINTS")){
                int httpResponse = wsResponse.getStatus();
                //log event
                Logger.of("upstream").trace("id_client:" + client.getIdClient() + " user_id:" + client.getUserId() + " status:"+httpResponse);
            }//else skip
        }catch (Exception ex){
            //do nothing catch to avoid interruptions
        }
    }

    private static void upstreamRequestLoggersubscribe(String msisdn, ObjectNode metadata, String eventType, String url) {
        try {
            Logger.of("upstream_subscribe").trace(eventType + " url: " + url + " msisdn:" + msisdn + " metadata: "+metadata.toString());
        }catch (Exception ex){
            //do nothing catch to avoid interruptions
        }
    }

    private static void upstreamResponseLoggersubscribe(String msisdn, WSResponse wsResponse, ObjectNode responseBody, String eventType){
        try {
            int httpResponse = wsResponse.getStatus();
            Logger.of("upstream_subscribe").trace(eventType + " msisdn:" + msisdn + " status: "+ httpResponse  + " response: " + responseBody);
        }catch (Exception ex){
            //do nothing catch to avoid interruptions
        }
    }
    
    private static void printRequest(WSRequestHolder urlCall, ObjectNode fields){
        System.out.println("-----------------------\nheaders: ");
        for (Map.Entry<String, Collection<String>> entry : urlCall.getHeaders().entrySet()) {
            System.out.println("\t" + entry.getKey() + " " + entry.getValue());
        }
        System.out.println("fields: " + fields + "\n-----------------------");
    }

}
