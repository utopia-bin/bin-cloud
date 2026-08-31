package cn.utopiabin.cloud.platform.service.iam;

import cn.utopiabin.cloud.platform.mapper.iam.SysMenuMapper;
import cn.utopiabin.cloud.platform.model.dto.iam.SysMenuCreateDTO;
import cn.utopiabin.cloud.platform.repository.iam.SysMenuRepository;
import cn.utopiabin.cloud.platform.service.PermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.mockito.Mockito.*;

class SysMenuCacheTest {
    private final PermissionService permissions = mock(PermissionService.class);
    private final SysMenuService service = new SysMenuService(mock(SysMenuRepository.class), mock(SysMenuMapper.class), permissions);

    @AfterEach
    void clearTransaction() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private void createMenu() {
        TransactionSynchronizationManager.initSynchronization();
        var dto = new SysMenuCreateDTO();
        dto.setName("新菜单");
        service.create(dto);
        verifyNoInteractions(permissions);
    }

    @Test
    void invalidatesMenusOnlyAfterCommit() {
        createMenu();
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        TransactionSynchronizationManager.getSynchronizations().forEach(
                synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        verify(permissions).evictAllUserPermissions();
    }

    @Test
    void rollbackKeepsExistingCache() {
        createMenu();
        TransactionSynchronizationManager.getSynchronizations().forEach(
                synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verifyNoInteractions(permissions);
    }
}
