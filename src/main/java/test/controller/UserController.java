package test.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import test.model.User;

@Path("/users")
public class UserController {

	private static final Logger logger = LoggerFactory.getLogger(UserController.class);
	
	private static Map<String,User> userStore = new ConcurrentHashMap<String,User>();

	@Context
    private UriInfo uriInfo;
	private final ObjectMapper om = new ObjectMapper();

	public UserController() {
		User u = new User();
		u.setDisplayName("Test");
		u.setPassword("12345");
		u.setUsername("User" + System.currentTimeMillis());
		userStore.put(u.getUsername(), u);
	}

	@GET
	@Produces("application/json")
	public Response find(@QueryParam("order") String order) {
		logger.debug("Executing method find()");
		Collection<User> users = userStore.values();
		List<User> results = new ArrayList<User>(users);
		if (order != null && !order.isEmpty()) {
			Comparator<User> c = new AscendingUserComparator();
			if (order.equals("desc")) {
				c = new DescendingUserComparator();
			}
			Collections.sort(results, c);
		}
		return Response.ok(results).build();
	}
	
	@GET
	@Path("/{username}")
	@Produces("application/json")
	public Response getByUsername(@PathParam("username") String username) {
		logger.debug("Executing method getByUsername()");
		if (username == null || username.isEmpty()) return Response.status(Status.NOT_FOUND).build();
		User result = userStore.get(username);
		if (result == null) return Response.status(Status.NOT_FOUND).build();
		return Response.ok(result).build();
	}

	@PUT
	@Path("/{username}")
	@Consumes({"application/json", "text/javascript", "text/plain"})
	public Response createPut(@PathParam("username") String username, String data) {
		logger.debug("Executing method createPut()");
		try {
			User user = om.readValue(data, User.class);
			userStore.put(username, user);
		} catch(Exception e) {
			logger.error("Could not create user: {}", e.getMessage());
			e.printStackTrace();
			return Response.serverError().build();
		}
		return Response.created(uriInfo.getAbsolutePathBuilder().build()).build();
	}

	/**
	 * Different (less standardish) way of creating a user
	 */
	@POST
	@Consumes("application/x-www-form-urlencoded")
	public Response createPost(@FormParam("username") String username, @FormParam("password") String password, @FormParam("displayName") String displayName) {
		logger.debug("Executing method createPost()");
		if (userStore.containsKey(username)) return Response.status(Status.CONFLICT).entity("User " + username + " already exists").build();
		User u = new User();
		u.setUsername(username);
		u.setDisplayName(displayName);
		u.setPassword(password);
		userStore.put(username, u);
		return Response.created(uriInfo.getAbsolutePathBuilder().path(username).build()).build();
	}
	
	@POST
	@Path("/{username}")
	@Consumes("application/x-www-form-urlencoded")
	public Response modify(@PathParam("username") String username, @FormParam("password") String password, @FormParam("displayName") String displayName) {
		logger.debug("Executing method modify()");
		if (username == null || username.isEmpty()) return Response.status(Status.NOT_FOUND).build();
		User u = userStore.get(username);
		if (u == null) return Response.status(Status.NOT_FOUND).build();
		u.setDisplayName(displayName);
		u.setPassword(password);
		return Response.ok("Success!").build();
	}
	
	@DELETE
	@Path("/{username}")
	public Response delete(@PathParam("username") String username) {
		logger.debug("Executing method delete()");
		if (username == null || username.isEmpty()) return Response.status(Status.NOT_FOUND).build();
		userStore.remove(username);
		return Response.accepted().build();
	}

	
	// utility classes
	
	private static class AscendingUserComparator implements Comparator<User> {
		@Override
		public int compare(User u1, User u2) {
			if (u1.getUsername() == null) return -1;
			return u1.getUsername().compareTo(u2.getUsername());
		}
	}

	private static class DescendingUserComparator implements Comparator<User> {
		@Override
		public int compare(User u1, User u2) {
			if (u1.getUsername() == null) return -1;
			return -1 * u1.getUsername().compareTo(u2.getUsername());
		}
	}

}