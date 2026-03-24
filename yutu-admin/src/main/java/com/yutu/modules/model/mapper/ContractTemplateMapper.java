package com.yutu.modules.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yutu.modules.model.entity.ContractTemplate;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ContractTemplateMapper extends BaseMapper<ContractTemplate> {
    @Update("UPDATE contract_template SET use_count = IFNULL(use_count, 0) + 1 WHERE id = #{id}")
    int incrementUseCount(@Param("id") Long id);

    @Update("UPDATE contract_template SET download_count = IFNULL(download_count, 0) + 1 WHERE id = #{id}")
    int incrementDownloadCount(@Param("id") Long id);
}
