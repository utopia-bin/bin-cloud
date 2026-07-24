package cn.utopiabin.cloud.platform.mapper.system;

import cn.utopiabin.cloud.platform.entity.system.SysDictOptions;
import cn.utopiabin.cloud.platform.model.vo.system.SysDictOptionsItemVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统字典项 Mapper
 *
 * @since 1.0
 */
@Mapper
public interface SysDictOptionsMapper extends BaseMapper<SysDictOptions> {

    /** 查询字典项列表 (携带字典编码) */
    List<SysDictOptionsItemVO> listWithCode(@Param("code") String dictCode);
}
