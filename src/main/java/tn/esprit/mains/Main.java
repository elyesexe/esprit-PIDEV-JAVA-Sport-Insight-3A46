package tn.esprit.mains;

import tn.esprit.tools.MyConnection;

public class Main {
    public static void main(String[] args) {
        try {
            MyConnection connection = MyConnection.getInstance();
            System.out.println("Connected to database: " + connection.getConnection().getCatalog());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
