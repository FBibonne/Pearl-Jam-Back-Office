package fr.insee.pearljam.api.controller;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.insee.pearljam.api.dto.user.UserDto;
import fr.insee.pearljam.api.exception.NotFoundException;
import fr.insee.pearljam.api.service.UserService;
import fr.insee.pearljam.api.service.UtilsService;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping(path = "/api")
public class UserController {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

	@Autowired
	UtilsService utilsService;

	@Autowired
	UserService userService;

	/**
	 * This method is using to get the current user informations
	 * 
	 * @param request
	 * @return List of {@link UserDto} if exist, {@link HttpStatus} NOT_FOUND, or
	 *         {@link HttpStatus} FORBIDDEN
	 */
	@ApiOperation(value = "Get User")
	@GetMapping(path = "/user")
	public ResponseEntity<UserDto> getUser(HttpServletRequest request) {
		String userId = utilsService.getUserId(request);
		if (StringUtils.isBlank(userId)) {
			LOGGER.info("GET User resulting in 403");
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		} else {
			UserDto user;
			try {
				user = userService.getUser(userId);
			}
			catch(NotFoundException e) {
				LOGGER.error(e.getMessage());
				LOGGER.info("GET User resulting in 403");
				return new ResponseEntity<>(HttpStatus.FORBIDDEN);
			}
			LOGGER.info("GET User resulting in 200");
			return new ResponseEntity<>(user, HttpStatus.OK);
		}
	}
	
	/**
	 * This method is using to delete an user
	 * 
	 * @param request
	 */
	@ApiOperation(value = "Delete User")
	@DeleteMapping(path = "/user/{id}")
	public ResponseEntity<Object> deleteUser(HttpServletRequest request, @PathVariable(value = "id") String id) {
		HttpStatus response = userService.delete(id);
		LOGGER.info("DELETE User resulting in {}", response);
		return new ResponseEntity<>(response);
	}
}
