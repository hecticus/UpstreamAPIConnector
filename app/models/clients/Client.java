package models.clients;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlUpdate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import controllers.Upstream;
import exceptions.MissingFieldsException;
import exceptions.UpstreamException;
import models.Config;
import models.HecticusModel;
import models.basic.Country;
import models.basic.Language;
import org.apache.commons.codec.binary.Base64;
import play.Logger;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import play.libs.Json;
import utils.DateAndTime;

import javax.persistence.*;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by plesse on 9/30/14.
 */
@Entity
@Inheritance
@DiscriminatorColumn(name="client_type", discriminatorType = DiscriminatorType.STRING)
@Table(name="clients")
public class Client extends HecticusModel {


    @Id
    protected Integer idClient;
    @Constraints.Required
    protected String userId;
    @Constraints.Required
    protected Integer status;
    @Constraints.Required
    protected String login;
    @Constraints.Required
    protected String password;
    @Constraints.Required
    @Constraints.MaxLength(value = 10)
    protected String lastCheckDate;

    protected String nickname;

    protected String facebookId;

    protected String session;

//    protected String clientType;

    @OneToOne
    @JoinColumn(name = "id_country")
    protected Country country;

    @OneToOne
    @JoinColumn(name = "id_language")
    protected Language language;

    @Version
    @Column(columnDefinition = "timestamp default '2014-10-06 21:17:06'")
    public Timestamp lastUpdate; // here

    @OneToMany(mappedBy="client", cascade = CascadeType.ALL)
    protected List<ClientHasDevices> devices;

    private static Model.Finder<Integer, Client> finder = new Model.Finder<>(Integer.class, Client.class);

    public Client(Integer status, String login, String password, Country country, Language language) {
        this.status = status;
        this.login = login;
        this.password = password;
        this.country = country;
        this.language = language;
    }

    public Client(String userId, Integer status, String login, String password, Country country, Language language) {
        this.userId = userId;
        this.status = status;
        this.login = login;
        this.password = password;
        this.country = country;
        this.language = language;
    }

    public Client(Integer status, String login, String password, Country country, String lastCheckDate, Language language) {
        this.status = status;
        this.login = login;
        this.password = password;
        this.country = country;
        this.lastCheckDate = lastCheckDate;
        this.language = language;
    }

    public Client(String userId, Integer status, String login, String password, Country country, String lastCheckDate, Language language) {
        this.userId = userId;
        this.status = status;
        this.login = login;
        this.password = password;
        this.country = country;
        this.lastCheckDate = lastCheckDate;
        this.language = language;
    }

    public Integer getIdClient() {
        return idClient;
    }

    public void setIdClient(Integer idClient) {
        this.idClient = idClient;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public List<ClientHasDevices> getDevices() {
        return devices;
    }

    public void setDevices(List<ClientHasDevices> devices) {
        this.devices = devices;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLastCheckDate() {
        return lastCheckDate;
    }

    public void setLastCheckDate(String lastCheckDate) {
        this.lastCheckDate = lastCheckDate;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public String getFacebookId() {
        return facebookId;
    }

    public void setFacebookId(String facebookId) {
        this.facebookId = facebookId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public Timestamp getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Timestamp lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

//    public String getClientType() {
//        return clientType;
//    }
//
//    public void setClientType(String clientType) {
//        this.clientType = clientType;
//    }

    public int getDeviceIndex(final String registrationId, final int deviceId) {
        ClientHasDevices clientHasDevice = null;
        try {
            clientHasDevice = Iterables.find(devices, new Predicate<ClientHasDevices>() {
                public boolean apply(ClientHasDevices obj) {
                    return obj.getRegistrationId().equalsIgnoreCase(registrationId) && obj.getDevice().getIdDevice().intValue() == deviceId;
                }
            });
        } catch (NoSuchElementException ex){
            clientHasDevice = null;
        }
        if(clientHasDevice == null){
            return -1;
        }
        return devices.indexOf(clientHasDevice);
    }

    public String getAuthToken(){
        String authString = login+":"+password;
        byte[] encodedBytes = Base64.encodeBase64(authString.getBytes());
        return new String(encodedBytes);
    }

    @Override
    public ObjectNode toJson() {
        ObjectNode response = Json.newObject();
        response.put("id_client", idClient);
        response.put("facebook_id", facebookId);
        response.put("nickname", nickname);
        response.put("user_id", userId);
        response.put("login", login);
        response.put("status", status);
        response.put("session", session);
        response.put("last_check_date", lastCheckDate);
        response.put("auth_token", getAuthToken());
        response.put("country", country.toJsonSimple());
        response.put("language", language.toJson());
        if(devices != null && !devices.isEmpty()){
            ArrayList<ObjectNode> apps = new ArrayList<>();
            for(ClientHasDevices ad : devices){
                apps.add(ad.toJsonWithoutClient());
            }
            response.put("devices", Json.toJson(apps));
        }

        return response;
    }

    public ObjectNode toJsonWithoutRelations() {
        ObjectNode response = Json.newObject();
        response.put("id_client", idClient);
        response.put("facebook_id", facebookId);
        response.put("nickname", nickname);
        response.put("user_id", userId);
        response.put("login", login);
        response.put("status", status);
        response.put("session", session);
        response.put("last_check_date", lastCheckDate);
        response.put("auth_token", getAuthToken());
        response.put("country", country.toJsonSimple());
        response.put("language", language.toJson());
        return response;
    }

    public ObjectNode toPMCJson() {
        ObjectNode response = Json.newObject();
        response.put("idClient", idClient);
        response.put("app", Config.getInt("pmc-id-app"));
        ArrayList<String> droid = new ArrayList<>();
        ArrayList<String> ios = new ArrayList<>();
        for(ClientHasDevices chd : devices){
            if(chd.getDevice().getName().endsWith("droid")){
                droid.add(chd.getRegistrationId());
            } else if(chd.getDevice().getName().endsWith("ios")){
                ios.add(chd.getRegistrationId());
            }
        }
        response.put("droid", Json.toJson(droid));
        response.put("ios", Json.toJson(ios));
        return response;
    }

    //Finder Operations


    public static Client getByID(int id){
        return finder.byId(id);
    }

    public static Client getByLogin(String login){
        return finder.where().eq("login",login).findUnique();
    }

    public static List<Client> getAll(){
        return finder.all();
    }

    public static List<Client> getPage(int page, int pageSize) {
        return Client.finder.where().setFirstRow(page).setMaxRows(pageSize).findList();
    }

    public static List<Client> getFriends(String[] friendsArray){
        return finder.where().in("facebookId", friendsArray).findList();
    }

    //Basic Operations

    public void checkStatus(String upstreamChannel) throws ParseException, UpstreamException {
        if(this.getStatus() >= 0) {
            int upstreamValidationTime = Config.getInt("upstream-validation-time");
            String lastCheckDateString = this.getLastCheckDate();
            TimeZone tz = TimeZone.getTimeZone("UTC");
            Calendar lastCheckDate = new GregorianCalendar(tz);
            lastCheckDate.setTime(DateAndTime.getDate(lastCheckDateString.length() == 8 ? lastCheckDateString + "000000" : lastCheckDateString, "yyyyMMddHHmmss", tz));
            Calendar actualDate = new GregorianCalendar(tz);
            Calendar checkDate = new GregorianCalendar(tz);
            checkDate.add(Calendar.MINUTE, -upstreamValidationTime);
            if(lastCheckDate.getTime().before(checkDate.getTime())){
                SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
                sf.setTimeZone(tz);
                if (this.login != null) {
                    Upstream.getStatusFromUpstream(this, upstreamChannel);
                    this.setLastCheckDate(sf.format(actualDate.getTime()));
                    try{
                        this.update();
                    } catch (OptimisticLockException ex){
                        //i dont care fo OptimisticLockException
                    }
                } else {
                    long daysBetween = DateAndTime.daysBetween(lastCheckDate, actualDate);
                    if (daysBetween >= Config.getInt("free-days")) {
                        this.setStatus(-1);
                        this.setLastCheckDate(sf.format(actualDate.getTime()));
                        this.update();
                    }
                }
            }
        }
    }

    public static Client create(String clientType, ObjectNode clientData) throws Exception {
        if (clientData.has("country") && clientData.has("language")) {
            Client client = null;
            String login = null;
            String password = null;
            //Obtenemos el canal por donde esta llegando el request
            String upstreamChannel;
            List<ClientHasDevices> otherRegsIDs = null;
            if(clientData.has("upstreamChannel")){
                upstreamChannel = clientData.get("upstreamChannel").asText();
            }else{
                upstreamChannel = "Android"; //"Android" o "Web"
            }
            if(clientData.has("login")){
                login = clientData.get("login").asText();

            }
            if(clientData.has("password")){
                password = clientData.get("password").asText();
            }
            UUID session = UUID.randomUUID();

            int countryId = clientData.get("country").asInt();
            Country country = Country.finder.byId(countryId);
            int languageId = clientData.get("language").asInt();
            Language language = Language.finder.byId(languageId);
            if (country != null && language != null) {
                TimeZone tz = TimeZone.getDefault();
                Calendar actualDate = new GregorianCalendar(tz);
                SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
                String date = sf.format(actualDate.getTime());

                client = new Client(2, login, password, country, date, language);
                //guardo client
                ArrayList<ClientHasDevices> devices = new ArrayList<>();
                if(clientData.has("devices")) {
                    Iterator<JsonNode> devicesIterator = clientData.get("devices").elements();
                    while (devicesIterator.hasNext()) {
                        ObjectNode next = (ObjectNode) devicesIterator.next();
                        if (next.has("device_id") && next.has("registration_id")) {
                            String registrationId = next.get("registration_id").asText();
                            int deviceId = next.get("device_id").asInt();
                            Device device = Device.finder.byId(deviceId);
                            if (device != null) {
                                ClientHasDevices clientHasDevice = new ClientHasDevices(client, device, registrationId);
                                //guardo clientHasDevice
                                devices.add(clientHasDevice);
                                otherRegsIDs = ClientHasDevices.finder.where().eq("registrationId", registrationId).eq("device", device).findList();
                                if (otherRegsIDs != null && !otherRegsIDs.isEmpty()) {
                                    for (ClientHasDevices clientHasDevices : otherRegsIDs) {
                                        clientHasDevices.delete();
                                    }
                                }
                            }
                        }
                    }
                } else {
                    int webDeviceId = Config.getInt("web-device-id");
                    Device device = Device.finder.byId(webDeviceId);
                    ClientHasDevices clientHasDevice = new ClientHasDevices(client, device, UUID.randomUUID().toString());
                    devices.add(clientHasDevice);
                }
                client.setDevices(devices);

                if (client.getPassword() != null && !client.getPassword().isEmpty()) {
                    Logger.of("upstream_subscribe").trace("status: " + client.toJson());
                    Upstream.getUserIdFromUpstream(client, upstreamChannel);
                    //borrar client
                } else {
                    Logger.of("upstream_subscribe").trace("subscribe: " + client.toJson());
                    Upstream.subscribeUserToUpstream(client, upstreamChannel);
                    //borrar client
                }
                Upstream.getStatusFromUpstream(client, upstreamChannel);

                client.setSession(session.toString());

                if(clientData.has("facebook_id")){
                    client.setFacebookId(clientData.get("facebook_id").asText());
                }

                if(clientData.has("nickname")){
                    client.setNickname(clientData.get("nickname").asText());
                }

                client.save();

                String sql = "update clients set `client_type` = \"" + clientType + "\" where `id_client` = " + client.getIdClient();
                SqlUpdate update = Ebean.createSqlUpdate(sql);
                int modifiedCount = Ebean.execute(update);
                
                return client;
            } else {
                throw new MissingFieldsException("Falta el country o el idioma");
            }
        } else {
            throw new MissingFieldsException("Falta el country o el idioma");
        }
    }

    public static Client update(int id, ObjectNode clientData) throws UpstreamException {
        Client client = getByID(id);
        if(client != null) {
            if(client.getStatus().intValue() == 2) {
                if (clientData.has("login")) {
                    Client clienttemp = Client.finder.where().eq("login", clientData.get("login").asText()).findUnique();
                    if (clienttemp != null && clienttemp.getIdClient().intValue() != client.getIdClient().intValue()) {
                        client.delete();
                        client = clienttemp;
                    }
                    if(client == null){
                        return null;
                    }
                }
            }
            boolean update = false;
            boolean loginAgain = false;
            if(clientData.has("login")){
                client.setLogin(clientData.get("login").asText());
                loginAgain = true;
                update = true;
            }

            if(clientData.has("password")){
                client.setPassword(clientData.get("password").asText());
                loginAgain = true;
                update = true;
            }

            String upstreamChannel = "Android"; //default Android
            if(clientData.has("upstreamChannel")){
                upstreamChannel = clientData.get("upstreamChannel").asText();
            }

            if(clientData.has("language")){
                int languageId = clientData.get("language").asInt();
                Language language = Language.finder.byId(languageId);
                if(language != null){
                    client.setLanguage(language);
                    update = true;
                }
            }

            if(clientData.has("remove_devices")){
                Iterator<JsonNode> devicesIterator = clientData.get("remove_devices").elements();
                ArrayList<ClientHasDevices> devices = new ArrayList<>();
                while (devicesIterator.hasNext()) {
                    ObjectNode next = (ObjectNode) devicesIterator.next();
                    if (next.has("device_id") && next.has("registration_id")) {
                        String registrationId = next.get("registration_id").asText();
                        int deviceId = next.get("device_id").asInt();
                        Device device = Device.finder.byId(deviceId);
                        if (device != null) {
                            int index = client.getDeviceIndex(registrationId, deviceId);
                            if(index != -1) {
                                client.getDevices().remove(index);
                                update = true;
                            }
                        }
                    }
                }
            }

            if(clientData.has("add_devices")) {
                Iterator<JsonNode> devicesIterator = clientData.get("add_devices").elements();
                List<ClientHasDevices> otherRegsIDs = null;
                while (devicesIterator.hasNext()) {
                    ObjectNode next = (ObjectNode) devicesIterator.next();
                    if (next.has("device_id") && next.has("registration_id")) {
                        String registrationId = next.get("registration_id").asText();
                        int deviceId = next.get("device_id").asInt();
                        Device device = Device.finder.byId(deviceId);
                        if (device != null) {
                            if(client.getDeviceIndex(registrationId, deviceId) == -1) {
                                ClientHasDevices clientHasDevice = new ClientHasDevices(client, device, registrationId);
                                client.getDevices().add(clientHasDevice);
                                update = true;
                            }
                        }
                        otherRegsIDs = ClientHasDevices.finder.where().ne("client.idClient", client.getIdClient()).eq("registrationId", registrationId).eq("device.idDevice", device.getIdDevice()).findList();
                        if(otherRegsIDs != null && !otherRegsIDs.isEmpty()){
                            for(ClientHasDevices clientHasDevices : otherRegsIDs){
                                clientHasDevices.delete();
                            }
                        }
                    }
                }
            }

            if(upstreamChannel.equalsIgnoreCase("web")){
                ClientHasDevices clientHasDevice;
                try {
                    clientHasDevice = Iterables.find(client.getDevices(), new Predicate<ClientHasDevices>() {
                        public boolean apply(ClientHasDevices obj) {
                            return obj.getDevice().getName().equalsIgnoreCase("web");
                        }
                    });
                } catch (NoSuchElementException ex){
                    clientHasDevice = null;
                }
                if(clientHasDevice == null) {
                    int webDeviceId = Config.getInt("web-device-id");
                    Device device = Device.finder.byId(webDeviceId);
                    clientHasDevice = new ClientHasDevices(client, device, UUID.randomUUID().toString());
                    client.getDevices().add(clientHasDevice);
                    update = true;
                }
            }

            if(loginAgain && (client.getLogin() != null && !client.getLogin().isEmpty()) && (client.getPassword() != null && !client.getPassword().isEmpty())){
                Upstream.getUserIdFromUpstream(client, upstreamChannel);
                Upstream.getStatusFromUpstream(client, upstreamChannel);
            }

            if(clientData.has("facebook_id")){
                String facebookId = clientData.get("facebook_id").asText();
                String clientFacebookId = client.getFacebookId();
                if(clientFacebookId == null || !facebookId.equalsIgnoreCase(clientFacebookId)) {
                    client.setFacebookId(facebookId);
                    update = true;
                }
            }

            if(clientData.has("nickname")){
                client.setNickname(clientData.get("nickname").asText());
                update = true;
            }

            //si pedimos que se suscriba debe hacerse
            if(clientData.has("subscribe") && clientData.has("login")){
                if(client != null){
                    if(client.getUserId() == null){
                        //tratamos de crear al cliente
                        Upstream.subscribeUserToUpstream(client, upstreamChannel);
                        update = true;
                    }
                    if(client.getStatus() <= 0){
                        Upstream.getStatusFromUpstream(client, upstreamChannel);
                        update = true;
                    }
                }
            }

            if(update){
                client.update();
            }
        }
        return client;
    }

    public static Client delete(int id){
        Client client = getByID(id);
        if(client != null) {
            client.delete();
        }
        return client;
    }

    public static Client getAndUpdate(String login, ObjectNode clientData) throws UpstreamException {
        Client client = finder.where().eq("login", login).findUnique();
        if (client != null) {
            String password = null;
            //Obtenemos el canal por donde esta llegando el request
            String upstreamChannel;
            List<ClientHasDevices> otherRegsIDs = null;
            if(clientData.has("upstreamChannel")){
                upstreamChannel = clientData.get("upstreamChannel").asText();
            }else{
                upstreamChannel = "Android"; //"Android" o "Web"
            }

            if(clientData.has("password")){
                password = clientData.get("password").asText();
            }
            UUID session = UUID.randomUUID();
            if (clientData.has("devices")) {
                Iterator<JsonNode> devicesIterator = clientData.get("devices").elements();
                while (devicesIterator.hasNext()) {
                    ObjectNode next = (ObjectNode) devicesIterator.next();
                    if (next.has("device_id") && next.has("registration_id")) {
                        String registrationId = next.get("registration_id").asText();
                        int deviceId = next.get("device_id").asInt();
                        Device device = Device.finder.byId(deviceId);
                        ClientHasDevices clientHasDevice = ClientHasDevices.finder.where().eq("client", client).eq("registrationId", registrationId).eq("device", device).findUnique();
                        if (clientHasDevice == null) {
                            clientHasDevice = new ClientHasDevices(client, device, registrationId);
                            client.getDevices().add(clientHasDevice);
                        }
                        otherRegsIDs = ClientHasDevices.finder.where().ne("client", client).eq("registrationId", registrationId).eq("device", device).findList();
                        if (otherRegsIDs != null && !otherRegsIDs.isEmpty()) {
                            for (ClientHasDevices clientHasDevices : otherRegsIDs) {
                                clientHasDevices.delete();
                            }
                        }
                    }
                }
            } else {
                int webDeviceId = Config.getInt("web-device-id");
                Device device = Device.finder.byId(webDeviceId);
                ClientHasDevices clientHasDevice = new ClientHasDevices(client, device, UUID.randomUUID().toString());
                client.getDevices().add(clientHasDevice);
            }
            if (client.getUserId() == null) {
                //si tenemos password tratamos de hacer login
                if (password != null && !password.isEmpty()) {
                    client.setPassword(password);
                    Upstream.getUserIdFromUpstream(client, upstreamChannel);
                } else {
                    //tratamos de crear al cliente
                    Upstream.subscribeUserToUpstream(client, upstreamChannel);
                }
            }
            //siempre que tengamos login y pass debemos revisar el status de upstream
            if (password != null && !password.isEmpty()) {
                client.setPassword(password);
                Upstream.getStatusFromUpstream(client, upstreamChannel);
            }

            if(clientData.has("facebook_id")){
                client.setFacebookId(clientData.get("facebook_id").asText());
            }

            if(clientData.has("nickname")){
                client.setNickname(clientData.get("nickname").asText());
            }

            client.setSession(session.toString());
            client.update();
        }
        return client;
    }

    public static Client getAndCheckStatus(int id, String upstreamChannel) throws ParseException, UpstreamException {
        Client client = Client.finder.byId(id);
        if(client != null) {
            if(client.getStatus() >= 0) {
                int upstreamValidationTime = Config.getInt("upstream-validation-time");
                String lastCheckDateString = client.getLastCheckDate();
                TimeZone tz = TimeZone.getTimeZone("UTC");
                Calendar lastCheckDate = new GregorianCalendar(tz);
                lastCheckDate.setTime(DateAndTime.getDate(lastCheckDateString.length() == 8 ? lastCheckDateString + "000000" : lastCheckDateString, "yyyyMMddHHmmss", tz));
                Calendar actualDate = new GregorianCalendar(tz);
                Calendar checkDate = new GregorianCalendar(tz);
                checkDate.add(Calendar.MINUTE, -upstreamValidationTime);
                if(lastCheckDate.getTime().before(checkDate.getTime())){
                    SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
                    sf.setTimeZone(tz);
                    if (client.getLogin() != null) {
                        Upstream.getStatusFromUpstream(client, upstreamChannel);
                        client.setLastCheckDate(sf.format(actualDate.getTime()));
                        client.update();
                    } else {
                        long daysBetween = DateAndTime.daysBetween(lastCheckDate, actualDate);
                        if (daysBetween >= Config.getInt("free-days")) {
                            client.setStatus(-1);
                            client.setLastCheckDate(sf.format(actualDate.getTime()));
                            client.update();
                        }
                    }
                }
            }
        }
        return client;
    }


    public static void remindPassword(Client client, ObjectNode clientData) throws Exception {
        String upstreamChannel;
        if(clientData.has("upstreamChannel")){
            upstreamChannel = clientData.get("upstreamChannel").asText();
        }else{
            upstreamChannel = "Android"; //"Android" o "Web"
        }
        Upstream.resetPasswordForUpstream(client, upstreamChannel);
    }
}

