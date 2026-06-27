package com.filecollection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.filecollection.entity.FileMetadata;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileMetadataMapper extends BaseMapper<FileMetadata> {
}
