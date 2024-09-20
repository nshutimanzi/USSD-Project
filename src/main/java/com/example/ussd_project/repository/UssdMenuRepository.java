package com.example.ussd_project.repository;

import com.example.ussd_project.model.UssdMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UssdMenuRepository extends JpaRepository<UssdMenu, Long> {
    List<UssdMenu> findByParentMenuId(String parentMenuId);
}
