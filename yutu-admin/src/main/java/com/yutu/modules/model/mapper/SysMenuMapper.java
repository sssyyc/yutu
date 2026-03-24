package com.yutu.modules.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yutu.modules.model.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {
    @Select("SELECT m.* FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sys_role r ON r.id = rm.role_id " +
            "WHERE r.role_code = #{roleCode} AND m.status = 1 ORDER BY m.sort_num ASC")
    List<SysMenu> selectByRoleCode(String roleCode);

    @Select("SELECT m.perms FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sys_role r ON r.id = rm.role_id " +
            "WHERE r.role_code = #{roleCode} AND m.status = 1 AND m.perms IS NOT NULL AND m.perms != ''")
    List<String> selectPermsByRoleCode(String roleCode);
}
