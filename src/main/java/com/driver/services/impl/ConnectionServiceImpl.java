package com.driver.services.impl;

import com.driver.model.*;
import com.driver.repository.ConnectionRepository;
import com.driver.repository.ServiceProviderRepository;
import com.driver.repository.UserRepository;
import com.driver.services.ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConnectionServiceImpl implements ConnectionService {
    @Autowired
    UserRepository userRepository2;
    @Autowired
    ServiceProviderRepository serviceProviderRepository2;
    @Autowired
    ConnectionRepository connectionRepository2;

    @Override
    public User connect(int userId, String countryName) throws Exception{
        User user = userRepository2.findById(userId).get();
        if(user.getMaskedIp()!=null){
            throw new Exception("Already connected");
        }
        else if(countryName.equalsIgnoreCase(user.getOriginalCountry().getCountryName().toString())){
            return user;
        }
        else {
            if (user.getServiceProviderList()==null){
                throw new Exception("Unable to connect");
            }
            List<ServiceProvider> serviceProviderList = user.getServiceProviderList();
            int x = Integer.MAX_VALUE;
            ServiceProvider serviceProvider=null;
            Country country=null;

            for(ServiceProvider sp:serviceProviderList){
                List<Country> countryList = sp.getCountryList();
                for (Country con: countryList){
                    if(countryName.equalsIgnoreCase(con.getCountryName().toString()) && x > sp.getId() ){
                        x=sp.getId();
                        serviceProvider=sp;
                        country=con;
                    }
                }
            }
            if (serviceProvider!=null){
                Connection connection = new Connection();
                connection.setUser(user);
                connection.setServiceProvider(serviceProvider);

                String cc = country.getCode();
                int givenId = serviceProvider.getId();
                String mask = cc+"."+givenId+"."+userId;

                user.setMaskedIp(mask);
                user.setConnected(true);
                user.getConnectionList().add(connection);

                serviceProvider.getConnectionList().add(connection);

                userRepository2.save(user);
                serviceProviderRepository2.save(serviceProvider);
            }
        }
        return user;
    }
    @Override
    public User disconnect(int userId) throws Exception {
        User user = userRepository2.findById(userId).get();
        if(user.getConnected()==true){
            throw new Exception("Already disconnected");
        }
        user.setMaskedIp(null);
        user.setConnected(false);
        userRepository2.save(user);
        return user;
    }
    @Override
    public User communicate(int senderId, int receiverId) throws Exception {
        User sender = userRepository2.findById(senderId).get();
        User reciever = userRepository2.findById(receiverId).get();

        if(reciever.getMaskedIp()!=null){
            String str = reciever.getMaskedIp();
            String cc = str.substring(0,3); //getting country code = cc from the whole maskedIp

            if(cc.equals(sender.getOriginalCountry().getCode())) {
                return sender;
            }
            else {
                String countryName = "";

                if (cc.equalsIgnoreCase(CountryName.IND.toCode())) {
                    countryName = CountryName.IND.toString();
                } else if (cc.equalsIgnoreCase(CountryName.USA.toCode())){
                    countryName = CountryName.USA.toString();
                }else if (cc.equalsIgnoreCase(CountryName.JPN.toCode())) {
                    countryName = CountryName.JPN.toString();
                }else if (cc.equalsIgnoreCase(CountryName.CHI.toCode())) {
                    countryName = CountryName.CHI.toString();
                }else if (cc.equalsIgnoreCase(CountryName.AUS.toCode())) {
                    countryName = CountryName.AUS.toString();
                }

                User user1 = connect(senderId,countryName);
                if (user1.getConnected()==true){
                    throw new Exception("Cannot establish communication");
                }
                else {
                    return user1;
                }
            }
        }
        else {
            if (reciever.getOriginalCountry().equals(sender.getOriginalCountry())) {
                return sender;
            }
            String countryName = reciever.getOriginalCountry().getCountryName().toString();
            User user2 = connect(senderId, countryName);
            if (user2.getConnected()==true) {
                throw new Exception("Cannot establish communication");
            } else{
                return user2;
            }
        }
    }
}
