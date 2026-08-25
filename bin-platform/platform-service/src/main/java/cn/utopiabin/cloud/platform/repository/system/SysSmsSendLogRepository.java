package cn.utopiabin.cloud.platform.repository.system;

import cn.utopiabin.cloud.platform.entity.system.SysSmsSendLog;
import cn.utopiabin.cloud.platform.mapper.system.SysSmsSendLogMapper;
import cn.utopiabin.cloud.platform.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public class SysSmsSendLogRepository extends BaseRepository<SysSmsSendLogMapper, SysSmsSendLog> {
    @Override
    protected String getNotFoundMessage() {
        return "短信发送日志不存在";
    }
}
