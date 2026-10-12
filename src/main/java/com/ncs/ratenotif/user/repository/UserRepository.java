package com.ncs.ratenotif.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ncs.ratenotif.user.entity.Roles;
import com.ncs.ratenotif.user.entity.UserEntity;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

	UserEntity findByUsername(String username);

	 List<UserEntity> findByRole(Roles role);

}
