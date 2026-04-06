package tn.esprit.services;

import tn.esprit.entities.User;

import java.sql.SQLException;
import java.util.List;

public interface IUserService {

    void addUser(User user) throws SQLException;

    void deleteUser(int id);

    User findUserById(int id);

    List<User> getAllUsers() throws SQLException;

    void updateUser(User user);

}