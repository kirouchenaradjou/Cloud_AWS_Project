package com.csye6255.web.application.fall2018.controller;

import com.csye6255.web.application.fall2018.dao.AttachmentDAO;
import com.csye6255.web.application.fall2018.dao.TransactionDAO;
import com.csye6255.web.application.fall2018.dao.UserDAO;
import com.csye6255.web.application.fall2018.pojo.Attachment;
import com.csye6255.web.application.fall2018.pojo.Transaction;
import com.csye6255.web.application.fall2018.pojo.User;

import com.csye6255.web.application.fall2018.utilities.AuthorizationUtility;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Controller
public class CommonController {

    private final static Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    UserDAO userDao;

    @Autowired
    TransactionDAO transactionDAO;

    @Autowired
    AttachmentDAO attachmentDAO;


    @RequestMapping(value = "/user/register", method = RequestMethod.POST, produces = {"application/json"},
            consumes = "application/json", headers = {"content-type=application/json; charset=utf-8"})
    @ResponseBody
    public String postRegister(@RequestBody User user) {

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        JsonObject jsonObject = new JsonObject();


        List<User> userList = userDao.findByUserName(user.getUserName());

        if (userList.size() == 0) {
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            String password = user.getPassword();
            String hashedPassword = passwordEncoder.encode(password);

            User u = new User();
            u.setUserName(user.getUserName());
            u.setPassword(hashedPassword);
            userDao.save(u);
            jsonObject.addProperty("message", "Registration Successful");


        } else jsonObject.addProperty("message", "User already exists!");

        return jsonObject.toString();


    }

    @RequestMapping(value = "/dontchangethis", method = RequestMethod.POST, produces = {"application/json"},
            consumes = "application/json", headers = {"content-type=application/json; charset=utf-8"})
    @ResponseBody
    public String postLogin(@RequestBody User user) {


        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        JsonObject jsonObject = new JsonObject();


        List<User> userList = userDao.findByUserName(user.getUserName());

        if (userList.size() != 0) {
            User foundUser = userList.get(0);

            System.out.println("User was found!! His username is " + foundUser.getUserName()
                    + " and his password is "
                    + foundUser.getPassword());

            if (encoder.matches(user.getPassword(), foundUser.getPassword())) {
                System.out.println("Size of the list is =" + userList.size());

                jsonObject.addProperty("message", "success");
                jsonObject.addProperty("time", new Date().toString());
            } else jsonObject.addProperty("message", "Incorrect Password");


        } else jsonObject.addProperty("message", "User not found!");

        return jsonObject.toString();


    }

    @RequestMapping(value = "/time", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String checkSession(@RequestHeader HttpHeaders headers, HttpServletRequest request) {
        JsonObject jsonObject = new JsonObject();
        final String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.toLowerCase().startsWith("basic")) {
            // Authorization: Basic base64credentials
            String base64Credentials = authorization.substring("Basic".length()).trim();
            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(credDecoded, StandardCharsets.UTF_8);
            // credentials = username:password
            final String[] values = credentials.split(":", 2);
            String userName = values[0];
            String password = values[1];
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            List<User> userList = userDao.findByUserName(userName);


            if (userList.size() != 0) {
                User user = userList.get(0);
                if (encoder.matches(password, user.getPassword())) {
                    jsonObject.addProperty("message", "Current Time is : " + new Date().toString());
                } else jsonObject.addProperty("message", "Incorrect Password");


            } else jsonObject.addProperty("message", "User not found! - Try Logging in again");

        } else
            jsonObject.addProperty("message", "You are not logged in!");


        return jsonObject.toString();

    }

    @RequestMapping(value ="/transaction", method =RequestMethod.GET, produces ="application/json")

    @ResponseBody
    public ResponseEntity getTransactions(@RequestHeader HttpHeaders headers, HttpServletRequest request) {
        final String authorization = request.getHeader("Authorization");
        JsonObject jsonObject = new JsonObject();
        if (authorization != null && authorization.toLowerCase().startsWith("basic")) {
            String[] values = AuthorizationUtility.getHeaderValues(authorization);
            String userName = values[0];
            String password = values[1];
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            List<User> userList = userDao.findByUserName(userName);
            List<JsonObject> jsonObjectList = new ArrayList<>();
            Gson gson = new Gson();
            if (userList.size() != 0) {
                User user = userList.get(0);
                if (encoder.matches(password, user.getPassword())) {
                    List<Transaction> transactionList = transactionDAO.findByUserId(user.getId());
                    if (((List) transactionList).size() != 0) {
                        for (Transaction transaction : transactionList) {
                            JsonObject jsonObject1 = new JsonObject();
                            jsonObject1.addProperty("id", transaction.getTransactionid());
                            jsonObject1.addProperty("description", transaction.getDescription());
                            jsonObject1.addProperty("amount", transaction.getAmount());
                            jsonObject1.addProperty("date", transaction.getDate());
                            jsonObject1.addProperty("merchant", transaction.getMerchant());
                            jsonObject1.addProperty("category", transaction.getCategory());
                            List<Attachment> attachmentList = attachmentDAO.findByTransaction(transaction);
                            JsonObject attachmentObj = new JsonObject();

                            if (attachmentList.size() != 0) {
                                for (Attachment attachment : attachmentList) {
                                    attachmentObj.addProperty("id", attachment.getAttachmentid());
                                    attachmentObj.addProperty("url", attachment.getUrl());
                                }
                            }
                            JsonElement attachmentJsonElement = gson.toJsonTree(attachmentObj);
                            jsonObject1.add("attachments", attachmentJsonElement);
                            jsonObjectList.add(jsonObject1);
                        }
                        return ResponseEntity.status(HttpStatus.OK).body(jsonObjectList.toString());

                    } else {
                        jsonObject.addProperty("message", "There is no transactions to show");
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(jsonObject.toString());
                    }

                } else {
                    jsonObject.addProperty("message", "Incorrect Password");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
                }


            } else {
                jsonObject.addProperty("message", "User not found! - Try Logging in again");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
            }

        } else {
            jsonObject.addProperty("message", "You are not logged in!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());

        }


    }

    @RequestMapping(value = "/transaction/{transactionid}/attachments", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity getAttachmentsByTransactionID(@PathVariable("transactionid") String transactionid, @RequestHeader HttpHeaders headers,
                                                        HttpServletRequest request) {
        final String authorization = request.getHeader("Authorization");
        JsonObject jsonObject = new JsonObject();
        if (authorization != null && authorization.toLowerCase().startsWith("basic")) {
            String[] values = AuthorizationUtility.getHeaderValues(authorization);
            String userName = values[0];
            String password = values[1];
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            List<User> userList = userDao.findByUserName(userName);
            List<JsonObject> jsonObjectList = new ArrayList<>();

            if (userList.size() != 0) {
                User user = userList.get(0);
                if (encoder.matches(password, user.getPassword())) {
                    List<Transaction> transactionList = transactionDAO.findByTransactionid(transactionid);
                    if (transactionList.size() != 0) {
                        Transaction trans = transactionList.get(0);
                        if (trans.getUser().getId() == user.getId()) {
                            List<Attachment> attachmentList = attachmentDAO.findByTransaction(trans);
                            if (attachmentList.size() != 0) {
                                for (Attachment attachment : attachmentList) {
                                    JsonObject attachmentObj = new JsonObject();
                                    attachmentObj.addProperty("id", attachment.getAttachmentid());
                                    attachmentObj.addProperty("url", attachment.getUrl());
                                    jsonObjectList.add(attachmentObj);
                                }
                                return ResponseEntity.status(HttpStatus.OK).body(jsonObjectList.toString());
                            } else {
                                jsonObject.addProperty("message", "There are no attachments on this transaction");
                                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(jsonObject.toString());
                            }
                        } else {
                            jsonObject.addProperty("message", "User not found! - Try Logging in again");
                            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
                        }
                    } else {
                        jsonObject.addProperty("message", "No transactions to show");
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(jsonObject.toString());
                    }
                } else {
                    jsonObject.addProperty("message", "Incorrect Password");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
                }
            } else {
                jsonObject.addProperty("message", "User not found! - Try Logging in again");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
            }
        } else {
            jsonObject.addProperty("message", "You are not logged in!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(jsonObject.toString());
        }
    }

}