package com.example.ussd_project.service;

import com.example.ussd_project.model.UssdMenu;
import com.example.ussd_project.repository.UssdMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UssdMenuService {

    private final UssdMenuRepository ussdMenuRepository;

    public List<UssdMenu> getMenuOptions(String parentMenuId){
        return ussdMenuRepository.findByParentMenuId(parentMenuId);
    }

    public String buildMenu(List<UssdMenu> menuOptions){
        StringBuilder menu = new StringBuilder();
        for (UssdMenu option : menuOptions){
            menu.append(option.getMenuOption()).append(".").append(option.getMenuTitle()).append("\n");
        }
        return menu.toString();
    }
}
