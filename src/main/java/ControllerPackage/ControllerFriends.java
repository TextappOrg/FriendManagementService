package ControllerPackage;


import DaoPackage.DaoFriends;
import DaoPackage.DaoFriendsInterface;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.naming.NamingException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutionException;


@Path("/Drug")
public class ControllerFriends {

    private DaoFriendsInterface daoFriendsInterface;

    @POST
    @Path("/Search")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findFriend(@FormParam("friendUid") String friendUid, @FormParam("adderUid") String adderUid){
        this.daoFriendsInterface = new DaoFriends(adderUid,friendUid);
        try {
            LinkedHashMap<String,String> responseMap = this.daoFriendsInterface.searchFriend();
            if (responseMap!= null){
                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(responseMap);
                return Response.ok(json, MediaType.APPLICATION_JSON).header("friendDetails", json).build();
            } else
                return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
        } catch (NamingException | JsonProcessingException | ExecutionException | InterruptedException e) {
            e.printStackTrace(); // TODO : debug
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Something wrong on our side").build();
        }
    }

    @POST
    @Path("/AddFriendRequest")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addFriendRequest(@FormParam("friendUid") String friendUid, @FormParam("adderUid") String adderUid){
        this.daoFriendsInterface = new DaoFriends(adderUid,friendUid);
        try {
            if(this.daoFriendsInterface.sendFriendRequest()) return Response.ok().build();
            else return Response.serverError().build();
        } catch (NamingException | ExecutionException | InterruptedException e) {
            e.printStackTrace(); // TODO : debug
            return Response.serverError().build();
        }
    }

    @POST
    @Path("/ConfirmFriendRequest")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response confirmFriendRequest(@FormParam("friendUid") String friendUid,
                                         @FormParam("adderUid") String adderUid,
                                         @FormParam("status") String status){
        this.daoFriendsInterface = new DaoFriends(adderUid,friendUid);
        try {
            HashSet<String> returnSet = this.daoFriendsInterface.confirmFriendRequest(status);
            for(String s : returnSet){
                if(s.equalsIgnoreCase("Accepted")) return Response.ok(s,MediaType.TEXT_HTML).build();
                else if (s.equalsIgnoreCase("Rejected")) return  Response.ok(s,MediaType.TEXT_HTML).build();
                else if (s.equalsIgnoreCase("Please login to confirm and refrain from" +
                        " hacking")) return Response.noContent()
                        .entity("\"Please login to confirm and refrain from\"" +
                                "\" hacking\"").type(MediaType.TEXT_HTML).build();
                else return Response.noContent().entity("Something went wrong " + s).type(MediaType.TEXT_HTML).build();
            }
        } catch (NamingException e) {
            e.printStackTrace(); // TODO : debug
            return Response.serverError().build();
        }
        return Response.noContent().entity("Something went wrong").type(MediaType.TEXT_HTML).build();
    }

    @POST
    @Path("/UnFriend")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response unFriend(@FormParam("friendUid") String friendUid, @FormParam ("adderUid") String adderUid){
        this.daoFriendsInterface = new DaoFriends(adderUid,friendUid);
        try {
            return this.daoFriendsInterface.un_friend()
                    ?
                    Response.ok("Unfriended",MediaType.TEXT_HTML)
                            .build()
                    :
                    Response.status(Response.Status.NOT_FOUND)
                            .entity("either you are not logged in or not found in friend list")
                            .type(MediaType.TEXT_HTML)
                            .build();
        } catch (NamingException e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @POST
    @Path("/PopulateFriendList")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response showFriendList(@FormParam("adderUid") String adderUid){
        this.daoFriendsInterface = new DaoFriends(adderUid);
        try {
            ArrayList returnList = daoFriendsInterface.populateFriendsList();
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    String json = objectMapper.writeValueAsString(returnList);
                    return Response.ok(json).type(MediaType.APPLICATION_JSON).build();
                } catch (NullPointerException e) {
                    e.printStackTrace(); // TODO : debug
                    return Response.noContent().build();
                }
            } catch (JsonProcessingException e ){
                return Response.serverError().build();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace(); // TODO : debug
            return Response.serverError().build();
        }
    }


    @POST
    @Path("/ChangeUUID")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void changeUUID(@FormParam("oldUUID") String oldUUID, @FormParam("newUUID") String newUUID){
        try {
            DaoFriendsInterface.changeUUID(oldUUID,newUUID);
        } catch (NamingException e) {
            e.printStackTrace(); // TODO : debug
        }
    }

}
