package models.clients;

import com.avaje.ebean.Page;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.HecticusModel;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import play.libs.Json;
import scala.Tuple2;
import scala.collection.JavaConversions;
import scala.collection.mutable.Buffer;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.*;

/**
 * Created by plesse on 9/30/14.
 */
@Entity
@Table(name="devices")
public class Device extends HecticusModel {

    @Id
    private Integer idDevice;
    @Constraints.Required
    private String name;

    @OneToMany(mappedBy="device")
    private List<ClientHasDevices> clients;

    public static Model.Finder<Integer, Device> finder = new Model.Finder<>(Integer.class, Device.class);

    public void setIdDevice(Integer idDevice) {
        this.idDevice = idDevice;
    }

    public void setClients(List<ClientHasDevices> clients) {
        this.clients = clients;
    }

    public Device(String name) {
        this.name = name;
    }

    public Integer getIdDevice() {
        return idDevice;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ClientHasDevices> getClients() {
        return clients;
    }

    @Override
    public ObjectNode toJson() {
        ObjectNode response = Json.newObject();
        response.put("id_device", idDevice);
        response.put("name", name);
        return response;
    }

    public ObjectNode toJsonWithRegistrations() {
        ObjectNode response = Json.newObject();
        response.put("id_device", idDevice);
        response.put("name", name);
        if(clients != null && !clients.isEmpty()){
            ArrayList<ObjectNode> apps = new ArrayList<>();
            for(ClientHasDevices ad : clients){
                apps.add(ad.toJsonWithoutDevice());
            }
            response.put("clients", Json.toJson(apps));
        }
        return response;
    }

    //Finder Operations

    public static Device getByID(int id){
        return finder.byId(id);
    }

    public static Iterator<Device> getPage(int pageSize, int page){
        Iterator<Device> iterator = null;
        if(pageSize == 0){
            iterator = finder.all().iterator();
        }else{
            iterator = finder.where().setFirstRow(page).setMaxRows(pageSize).findList().iterator();
        }
        return  iterator;
    }

    public static scala.collection.immutable.List<Tuple2<String, String>> toSeq() {
        List<Device> devices = Device.finder.all();
        ArrayList<Tuple2<String, String>> proxy = new ArrayList<>();
        for(Device device : devices) {
            Tuple2<String, String> t = new Tuple2<>(device.getIdDevice().toString(), device.getName());
            proxy.add(t);
        }
        Buffer<Tuple2<String, String>> timezonesBuffer = JavaConversions.asScalaBuffer(proxy);
        scala.collection.immutable.List<Tuple2<String, String>> timezonesList = timezonesBuffer.toList();
        return timezonesList;
    }

    public static Map<String,String> options() {
        LinkedHashMap<String,String> options = new LinkedHashMap<String,String>();
        List<Device> devices = finder.all();
        for(Device l: devices) {
            options.put(l.getIdDevice().toString(), l.getName());
        }
        return options;
    }

    public static Page<Device> page(int page, int pageSize, String sortBy, String order, String filter) {
        return finder.where().orderBy(sortBy + " " + order).findPagingList(pageSize).getPage(page);
    }





}
