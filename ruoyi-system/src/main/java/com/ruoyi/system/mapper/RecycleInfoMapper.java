package com.ruoyi.system.mapper;

import com.ruoyi.common.core.domain.dto.RecycleInfoCondition;
import com.ruoyi.system.domain.RecycleInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RecycleInfoMapper
{
    List<RecycleInfo> search(RecycleInfoCondition _Condition);

    RecycleInfo findByUUID(@Param("uuid") String _UUID);

    int insert_batch(List<RecycleInfo> list);

    int insert(RecycleInfo recycleInfo);

    int delByUUID(List<String> list);
}
