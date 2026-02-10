package com.example.moattravel.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.moattravel.entity.House;

public interface HouseRepository extends JpaRepository<House, Integer> {

}
