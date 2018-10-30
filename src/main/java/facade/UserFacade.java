package facade;

import model.UserModel;
import model.InnerModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import model.ResponseModel;
import util.*;

public class UserFacade {
    
    private DBAccess db;
    private PropertiesReader pReader;
    private JacksonMapper jackson;
    private Validator validator;
    private static InnerModel in;
    
    public UserFacade(){
        db = null;
        pReader = null;
        jackson = null;
        validator = null;
    }

    public String insertUser(HttpServletRequest request) throws SQLException, JsonProcessingException{
        
        pReader = PropertiesReader.getInstance();
        db = new DBAccess(pReader.getValue("dbDriver"),pReader.getValue("dbUrl"),pReader.getValue("dbUser"),pReader.getValue("dbPassword"));
        jackson = new JacksonMapper();
        ResultSet rs = null;
        ResponseModel<InnerModel> res = new ResponseModel();
        String salt = Encrypter.getSalt(10);
        try{
            UserModel user = jackson.jsonToPojo(request,UserModel.class);
            if (isValidated(user.getUsername(),user.getPassword(),user.getEmail())){
                    rs = db.execute(pReader.getValue("q1"), user.getUsername(), user.getEmail());
                if(!rs.next()){
                    db.update(pReader.getValue("q2"),user.getUsername().toLowerCase(),Encrypter.getSecurePassword(user.getPassword() + salt),user.getName(),user.getEmail(),db.currentTimestamp(),2, salt);
                    res.setStatus("200");//Mensage
                }else{
                    res.setStatus("500");//Mensage
                }
                rs.close();
                db.close();
            }else{
               res.setStatus("403");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return jackson.pojoToJson(res);
        
        }    
    
    private InnerModel getUserData(HttpServletRequest request) throws SQLException{
        pReader = PropertiesReader.getInstance();
        db = new DBAccess(pReader.getValue("dbDriver"),pReader.getValue("dbUrl"),pReader.getValue("dbUser"),pReader.getValue("dbPassword"));
        jackson = new JacksonMapper();
        ResultSet rs = null;

        
        InnerModel dataUser = null; //new HashMap<>();
        
        try{
            dataUser = new InnerModel();
            UserModel user = jackson.jsonToPojo(request,UserModel.class);
            String salt = this.getUserSalt(db.execute(pReader.getValue("q1"), user.getUsername(), user.getUsername()));
            if (salt != null){
                rs = db.execute(pReader.getValue("q3"), Encrypter.getSecurePassword(user.getPassword() + salt),user.getUsername().toLowerCase(),user.getUsername().toLowerCase());

                if(rs.next()){
                    //Orden: id, type, password, username, name, creationtime, email
                    dataUser.setId(String.valueOf(rs.getInt(1)));
                    dataUser.setTypeuser(String.valueOf(rs.getInt(2)));
                    dataUser.setUsername(rs.getString(4));
                    dataUser.setName(rs.getString(5));
                    dataUser.setEmail(rs.getString(7));
                }
            }
            System.out.println(dataUser);
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            rs.close();
            db.close();
        }
        
        return dataUser;
        
    }
    
public HttpSession checkUser(HttpServletRequest request) throws JsonProcessingException{ //Este corrobora que el HashMap no este null para crear una session y retornarla
        HttpSession session = null;
        try {
            InnerModel userdata = getUserData(request);
            in = userdata;
            if(userdata!=null){
                session = request.getSession();
                validator = new Validator();
                validator.setSessionValues(session, userdata);
            }
        } catch (SQLException ex) {
            Logger.getLogger(UserFacade.class.getName()).log(Level.SEVERE, null, ex);
        }
    return session;
}

public InnerModel getSessionData(){
    return in;
}

public String getProperty(String propertyValue){
    pReader = PropertiesReader.getInstance();
    return pReader.getValue(propertyValue);
}

private boolean isValidated(String username, String password, String email){
    pReader = PropertiesReader.getInstance();
    validator = new Validator();
    
    if (validator.WhitespaceValidated(username, password, email) && validator.EmailContainsDomains(pReader.getValue("ER"), email)
            && !validator.hasSpecialCharacter(pReader.getValue("UR"), username) 
            && !validator.hasSpecialCharacter(pReader.getValue("PR"), password)
            && validator.LengthValidated(username, password, 20)){
        return true;
    }
    return false;
}

public <T> String writeJSON(T json) throws JsonProcessingException{
    jackson = new JacksonMapper();    
    return jackson.pojoToJson(json);
}

private String getUserSalt(ResultSet rs) throws IOException{
    ResultSet rs1 = rs;
    String salt = null;
    pReader = PropertiesReader.getInstance();
    db = new DBAccess(pReader.getValue("dbDriver"),pReader.getValue("dbUrl"),pReader.getValue("dbUser"),pReader.getValue("dbPassword"));
        try {
            if(rs1.next()){
                salt = rs1.getString(8);
                return salt;
            }   } catch (SQLException ex) {
            Logger.getLogger(UserFacade.class.getName()).log(Level.SEVERE, null, ex);
        }
    return salt;
}

}
