package controllers.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.UpstreamController;
import exceptions.UpstreamException;
import models.Config;
import models.basic.Language;
import models.clients.Client;
import models.clients.ClientHasDevices;
import play.libs.Json;
import play.mvc.Result;
import utils.UpstreamCoreUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by plessmann on 05/06/15.
 */
public class Clients extends UpstreamController {

    public static Result create() {
        ObjectNode clientData = getJson();
        try {
            Client client = null;
            String login = null;
            if(clientData.has("login")){
                login = clientData.get("login").asText();
            }
            if(login != null) {
                client = Client.getAndUpdate(login, clientData);
                if (client != null) {
                    return ok(buildBasicResponse(0, "OK", client.toJson()));
                }
            }



            client = Client.create("upstream", clientData);
            return created(buildBasicResponse(0, "OK", client.toJson()));
        } catch (Exception ex) {
            ObjectNode response;
            if(ex instanceof UpstreamException){
                UpstreamException upstreamException = (UpstreamException) ex;
                UpstreamCoreUtils.printToLog(Clients.class, "Error manejando clients", "error creando client con params " + clientData + " el request fue " + upstreamException.getRequest(), true, ex, "support-level-1", Config.LOGGER_ERROR);
                response = buildUpstreamResponse(-2, "ocurrio un error creando el registro", upstreamException);
            } else {
                UpstreamCoreUtils.printToLog(Clients.class, "Error manejando clients", "error creando client con params " + clientData, true, ex, "support-level-1", Config.LOGGER_ERROR);
                response = buildBasicResponse(-1, "ocurrio un error creando el registro", ex);
            }
            return internalServerError(response);
        }
    }

    public static Result update(Integer id) {
        ObjectNode clientData = getJson();
        try{
            Client client = Client.update(id, clientData);
            if(client != null) {
                return ok(buildBasicResponse(0, "OK", client.toJson()));
            } else {
                return notFound(buildBasicResponse(2, "no existe el cliente " + id));
            }
        } catch (Exception ex) {
            ObjectNode response;
            if(ex instanceof UpstreamException){
                UpstreamException upstreamException = (UpstreamException) ex;
                UpstreamCoreUtils.printToLog(Clients.class, "Error manejando clients", "error actualizando client con params " + clientData + " el request fue " + upstreamException.getRequest(), true, ex, "support-level-1", Config.LOGGER_ERROR);
                response = buildUpstreamResponse(-2, "ocurrio un error creando el registro", upstreamException);
            } else {
                UpstreamCoreUtils.printToLog(Clients.class, "Error manejando clients", "error actualizando client con params " + clientData, true, ex, "support-level-1", Config.LOGGER_ERROR);
                response = buildBasicResponse(-1, "ocurrio un error creando el registro", ex);
            }
            return internalServerError(response);
        }
    }

    public static Result delete(Integer id) {
        try{
            Client client = Client.delete(id);
            if(client != null) {
                return ok(buildBasicResponse(0, "OK", client.toJson()));
            } else {
                return notFound(buildBasicResponse(2, "no existe el cliente " + id));
            }
        } catch (Exception ex) {
            UpstreamCoreUtils.printToLog(Clients.class, "Error manejando clients", "error eliminando el client " + id, true, ex, "support-level-1", Config.LOGGER_ERROR);
            return internalServerError(buildBasicResponse(3, "ocurrio un error eliminando el registro", ex));
        }
    }

    public static Result get(Integer id, String upstreamChannel, Boolean pmc){
        try {
            Client client = null;
            if(pmc){
                client = Client.getByID(id);
            } else {
                client = Client.getAndCheckStatus(id, upstreamChannel);
            }
            if(client != null) {
                return ok(buildBasicResponse(0, "OK", pmc?client.toPMCJson():client.toJson()));
            } else {
                return notFound(buildBasicResponse(2, "no existe el registro a consultar"));
            }
        }catch (Exception ex) {
            ObjectNode response;
            if(ex instanceof UpstreamException){
                UpstreamException upstreamException = (UpstreamException) ex;
                UpstreamCoreUtils.printToLog(Clients.class, "Error manejando clients", "error obteniendo el client " + id + " el request fue " + upstreamException.getRequest(), true, ex, "support-level-1", Config.LOGGER_ERROR);
                response = buildUpstreamResponse(-2, "ocurrio un error creando el registro", upstreamException);
            } else {
                UpstreamCoreUtils.printToLog(Clients.class, "Error manejando clients", "error obteniendo el client " + id, true, ex, "support-level-1", Config.LOGGER_ERROR);
                response = buildBasicResponse(-1, "ocurrio un error creando el registro", ex);
            }
            return internalServerError(response);
        }
    }

    public static Result list(Integer pageSize,Integer page, Boolean pmc){
        try {

            Iterator<Client> clientIterator = null;
            if(pageSize == 0){
                clientIterator = Client.getAll().iterator();
            }else{
                clientIterator = Client.getPage(page, pageSize).iterator();
            }

            ArrayList<ObjectNode> clients = new ArrayList<>();
            while(clientIterator.hasNext()){
                clients.add(pmc?clientIterator.next().toPMCJson():clientIterator.next().toJson());
            }
            return ok(buildBasicResponse(0, "OK", Json.toJson(clients)));
        }catch (Exception e) {
            UpstreamCoreUtils.printToLog(Clients.class, "Error manejando clients", "error listando clients con pageSize " + pageSize + " y " + page, true, e, "support-level-1", Config.LOGGER_ERROR);
            return internalServerError(buildBasicResponse(-1,"Error buscando el registro",e));
        }
    }

    public static Result cleanDevices(){
        ObjectNode json = getJson();
        try{
            Iterator<JsonNode> operations = json.get("operations").elements();
            while(operations.hasNext()) {
                ObjectNode operation = (ObjectNode) operations.next();
                if(operation.has("type") && operation.has("actual_id") && operation.has("operation")) {
                    String type = operation.get("type").asText();
                    String actualId = operation.get("actual_id").asText();
                    String action = operation.get("operation").asText();
                    List<ClientHasDevices> devs = ClientHasDevices.finder.where().eq("registrationId", actualId).eq("device.name",type).findList();
                    for(ClientHasDevices d : devs) {
                        if(action.equalsIgnoreCase("UPDATE")){
                            d.setRegistrationId(operation.get("new_id").asText());
                            d.update();
                        } else if(action.equalsIgnoreCase("DELETE")){
                            d.delete();
                        }
                    }
                    if(operation.has("new_id")) {
                        devs = ClientHasDevices.finder.where().eq("registrationId", operation.get("new_id").asText()).eq("device.name",type).findList();
                        if(devs != null && !devs.isEmpty()){
                            for(int i = 1; i < devs.size(); ++i){
                                devs.get(i).delete();
                            }
                        }
                    }
                }
            }
            return ok(buildBasicResponse(0,"ok"));
        }catch(Exception ex){
            UpstreamCoreUtils.printToLog(Clients.class, "Error manejando clients", "Ocurrio un error limpiando los devices " + json, true, ex, "support-level-1", Config.LOGGER_ERROR);
            return internalServerError(buildBasicResponse(1, "Error buscando el registro", ex));
        }
    }

    public static Result getActiveLanguages(){
        try {
            List<Language> activeLanguages = Language.getActiveLanguages();
            if(activeLanguages != null && !activeLanguages.isEmpty()) {
                ArrayList<ObjectNode> languages = new ArrayList<>();
                for(Language language : activeLanguages){
                    languages.add(language.toJson());
                }
                ObjectNode responseData = Json.newObject();
                responseData.put("languages", Json.toJson(languages));
                return ok(buildBasicResponse(0, "OK", responseData));
            } else {
                return notFound(buildBasicResponse(2, "no hay idiomas activos"));
            }
        }catch (Exception e) {
            UpstreamCoreUtils.printToLog(Clients.class, "Error manejando Idiomas", "error obteniendo los idiomas activos ", true, e, "support-level-1", Config.LOGGER_ERROR);
            return internalServerError(buildBasicResponse(1,"Error buscando idiomas",e));
        }
    }
    
}
