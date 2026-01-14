package com.mortal.user.service;

import com.mortal.user.entity.Role;
import java.util.List;

public interface RoleService {

    void bindRole(Long userId, Long roleId);

    List<Role> listRoles();

    List<Long> listUserRoleIds(Long userId);
}
