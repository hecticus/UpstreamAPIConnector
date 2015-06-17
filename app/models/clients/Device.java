package models.clients;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.HecticusModel;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import play.libs.Json;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

}
