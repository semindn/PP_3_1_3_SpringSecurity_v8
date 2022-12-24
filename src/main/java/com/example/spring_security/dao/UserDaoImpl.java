package com.example.spring_security.dao;

import com.example.spring_security.entity.Role;
import com.example.spring_security.entity.User;
import javax.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class UserDaoImpl implements UserDao{

    @PersistenceContext
    private EntityManager entityManager;

    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    public UserDaoImpl(BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Override
    public List<User> getUsers() {
        return entityManager.createQuery("select u from User u", User.class).getResultList();
    }

    @Override
    public Set<Role> getAllRoles() {
        return new HashSet<>(
                entityManager.createQuery("select r from Role r", Role.class).getResultList()
        );
    }

    @Override
    public Role getRoleByName(String name){
        TypedQuery<Role> roleName = entityManager.createQuery("select r from Role r where r.name = :name", Role.class);
        roleName.setParameter("name", name);
        return roleName.getResultList().stream().findFirst().orElse(null);
    }

    @Override
    @Transactional
    public void saveUser(User user) {
        User newUser = new User();
        newUser.setAge(user.getAge());
        newUser.setEnabled(user.isEnabled());
        newUser.setFirstName(user.getFirstName());
        newUser.setLastName(user.getLastName());
        newUser.setLogin(user.getLogin());
        //присваиваем объекту newUser роль USER если у объекта user, переданного в метод пустая роль
        //иначе перезаписываем имеющиеся в user роли в объект newUser
        if (user.getRoles().isEmpty()){
            newUser.addRole(getRoleByName("ROLE_USER"));
        } else {
            Set<Role> roles = user.getRoles();
            for (Role roleInSet : roles) {
                newUser.addRole(getRoleByName(roleInSet.getName()));
            }
        }

        // Если объект user имеет id, обновляем имеющуюся запись в БД, иначе сохраняем новую запись
        if (user.getId() == null) {
            newUser.setPassw(bCryptPasswordEncoder.encode(user.getPassword()));
            entityManager.persist(newUser);
        } else {
            newUser.setId(user.getId());
            // если пароль пришел пустой - его не меняли получаем хеш зашифрованного пароля по id объекта user
            // и устанавливаем его для newUser, иначе шифруем String и устанавливаем паролем получившийся хеш для newUser
            if (user.getPassword()==null) {
                newUser.setPassw(getSingleUserById(user.getId()).getPassword());
            } else {
                newUser.setPassw(bCryptPasswordEncoder.encode(user.getPassword()));
            }
            entityManager.merge(newUser);
        }
    }

//    @Override
//    public User getSingleUserById(Long id) {
//        TypedQuery<User> typedQuery = entityManager.createQuery("select u from User u where u.id = :id", User.class);
//        typedQuery.setParameter("id", id);
//        User userFromDB = typedQuery.getResultList().stream().findFirst().orElse(null);
//        //для передачи объекта пользователя во вне - затираем его пароль
//        userFromDB.setPassw(null);
//        return userFromDB;
//    }

    @Override
    public User getSingleUserById(Long id) {
        TypedQuery<User> typedQuery = entityManager.createQuery("select u from User u where u.id = :id", User.class);
        typedQuery.setParameter("id", id);
        User user = typedQuery.getResultList().stream().findFirst().orElse(null);
        user.setPassw(null);
        return user;
    }

    @Override
    public User getSingleUserByLogin(String login) {
        TypedQuery<User> typedQuery = entityManager.createQuery("select u from User u where u.login = :login", User.class);
        typedQuery.setParameter("login", login);
        return typedQuery.getResultList().stream().findFirst().orElse(null);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        entityManager.remove(getSingleUserById(id));
    }
}
