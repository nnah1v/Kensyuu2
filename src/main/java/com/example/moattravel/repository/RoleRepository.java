package com.example.moattravel.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.moattravel.entity.Roles;

public interface RoleRepository extends JpaRepository<Roles, Integer> {
	public Roles findByName(String name);

}
