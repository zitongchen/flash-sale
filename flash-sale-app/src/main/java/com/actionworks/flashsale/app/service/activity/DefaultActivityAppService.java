package com.actionworks.flashsale.app.service.activity;

import com.actionworks.flashsale.app.auth.AuthorizationService;
import com.actionworks.flashsale.app.auth.model.AuthResult;
import com.actionworks.flashsale.app.exception.BizException;
import com.actionworks.flashsale.app.model.builder.FlashActivityAppBuilder;
import com.actionworks.flashsale.app.model.command.FlashActivityPublishCommand;
import com.actionworks.flashsale.app.model.dto.FlashActivityDTO;
import com.actionworks.flashsale.app.model.query.FlashActivitiesQuery;
import com.actionworks.flashsale.app.model.result.AppMultiResult;
import com.actionworks.flashsale.app.model.result.AppResult;
import com.actionworks.flashsale.app.model.result.AppSimpleResult;
import com.actionworks.flashsale.app.service.activity.cache.FlashActivitiesCacheService;
import com.actionworks.flashsale.app.service.activity.cache.FlashActivityCacheService;
import com.actionworks.flashsale.app.service.activity.cache.model.FlashActivitiesCache;
import com.actionworks.flashsale.app.service.activity.cache.model.FlashActivityCache;
import com.actionworks.flashsale.controller.exception.AuthException;
import com.actionworks.flashsale.domain.model.PageResult;
import com.actionworks.flashsale.domain.model.entity.FlashActivity;
import com.actionworks.flashsale.domain.service.FlashActivityDomainService;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

import static com.actionworks.flashsale.app.auth.model.ResourceEnum.FLASH_ITEM_CREATE;
import static com.actionworks.flashsale.app.exception.AppErrorCode.ACTIVITY_NOT_FOUND;
import static com.actionworks.flashsale.app.exception.AppErrorCode.INVALID_PARAMS;
import static com.actionworks.flashsale.app.model.builder.FlashActivityAppBuilder.toDomain;
import static com.actionworks.flashsale.app.model.builder.FlashActivityAppBuilder.toFlashActivitiesQuery;
import static com.actionworks.flashsale.controller.exception.ErrorCode.UNAUTHORIZED_ACCESS;

@Service
public class DefaultActivityAppService implements FlashActivityAppService {
    private static final Logger logger = LoggerFactory.getLogger(DefaultActivityAppService.class);

    @Resource
    private FlashActivityDomainService flashActivityDomainService;

    @Resource
    private AuthorizationService authorizationService;

    @Resource
    private FlashActivityCacheService flashActivityCacheService;
    @Resource
    private FlashActivitiesCacheService flashActivitiesCacheService;

    @Override
    public AppResult publishFlashActivity(Long userId, FlashActivityPublishCommand flashActivityPublishCommand) {
        logger.info("activityPublish|发布秒杀活动|{},{}", userId, JSON.toJSONString(flashActivityPublishCommand));
        if (userId == null || flashActivityPublishCommand == null || !flashActivityPublishCommand.validate()) {
            throw new BizException(INVALID_PARAMS);
        }
        AuthResult authResult = authorizationService.auth(userId, FLASH_ITEM_CREATE);
        if (!authResult.isSuccess()) {
            throw new AuthException(UNAUTHORIZED_ACCESS);
        }
        flashActivityDomainService.publishActivity(userId, toDomain(flashActivityPublishCommand));
        logger.info("activityPublish|活动已发布");
        return AppResult.buildSuccess();
    }

    @Override
    public AppResult modifyFlashActivity(Long userId, Long activityId, FlashActivityPublishCommand flashActivityPublishCommand) {
        logger.info("activityModification|秒杀活动修改|{},{},{}", userId, activityId, JSON.toJSONString(flashActivityPublishCommand));
        if (userId == null || flashActivityPublishCommand == null || !flashActivityPublishCommand.validate()) {
            throw new BizException(INVALID_PARAMS);
        }
        AuthResult authResult = authorizationService.auth(userId, FLASH_ITEM_CREATE);
        if (!authResult.isSuccess()) {
            throw new AuthException(UNAUTHORIZED_ACCESS);
        }
        FlashActivity flashActivity = toDomain(flashActivityPublishCommand);
        flashActivity.setId(activityId);
        flashActivityDomainService.modifyActivity(userId, flashActivity);
        logger.info("activityModification|活动已修改");
        return AppResult.buildSuccess();
    }

    @Override
    public AppResult onlineFlashActivity(Long userId, Long activityId) {
        logger.info("activityOnline|上线活动|{},{}", userId, activityId);
        if (userId == null || activityId == null) {
            throw new BizException(INVALID_PARAMS);
        }
        AuthResult authResult = authorizationService.auth(userId, FLASH_ITEM_CREATE);
        if (!authResult.isSuccess()) {
            throw new AuthException(UNAUTHORIZED_ACCESS);
        }
        flashActivityDomainService.onlineActivity(userId, activityId);
        logger.info("activityOnline|活动已上线");
        return AppResult.buildSuccess();
    }

    @Override
    public AppResult offlineFlashActivity(Long userId, Long activityId) {
        logger.info("activityOffline|下线活动|{},{}", userId, activityId);
        if (userId == null || activityId == null) {
            throw new BizException(INVALID_PARAMS);
        }
        AuthResult authResult = authorizationService.auth(userId, FLASH_ITEM_CREATE);
        if (!authResult.isSuccess()) {
            throw new AuthException(UNAUTHORIZED_ACCESS);
        }
        flashActivityDomainService.offlineActivity(userId, activityId);
        logger.info("activityOffline|活动已下线");
        return AppResult.buildSuccess();
    }

    @Override
    public AppMultiResult<FlashActivityDTO> getFlashActivities(Long userId, FlashActivitiesQuery flashActivitiesQuery) {
        List<FlashActivity> activities;
        Integer total;
        if (flashActivitiesQuery.isFirstPureQuery()) {
            FlashActivitiesCache flashActivitiesCache = flashActivitiesCacheService.getCachedActivities(flashActivitiesQuery.getPageNumber(), flashActivitiesQuery.getVersion());
            if (flashActivitiesCache.isLater()) {
                return AppMultiResult.tryLater();
            }
            activities = flashActivitiesCache.getFlashActivities();
            total = flashActivitiesCache.getTotal();
        } else {
            PageResult<FlashActivity> flashActivityPageResult = flashActivityDomainService.getFlashActivities(toFlashActivitiesQuery(flashActivitiesQuery));
            activities = flashActivityPageResult.getData();
            total = flashActivityPageResult.getTotal();
        }

        List<FlashActivityDTO> flashActivityDTOList = activities.stream().map(FlashActivityAppBuilder::toFlashActivityDTO).collect(Collectors.toList());
        return AppMultiResult.of(flashActivityDTOList, total);
    }

    @Override
    public AppSimpleResult<FlashActivityDTO> getFlashActivity(Long userId, Long activityId, Long version) {
        if (userId == null || activityId == null) {
            throw new BizException(INVALID_PARAMS);
        }
        FlashActivityCache flashActivityCache = flashActivityCacheService.getCachedActivity(activityId, version);
        if (!flashActivityCache.isExist()) {
            throw new BizException(ACTIVITY_NOT_FOUND.getErrDesc());
        }
        if (flashActivityCache.isLater()) {
            return AppSimpleResult.tryLater();
        }
        FlashActivityDTO flashActivityDTO = FlashActivityAppBuilder.toFlashActivityDTO(flashActivityCache.getFlashActivity());
        flashActivityDTO.setVersion(flashActivityCache.getVersion());
        return AppSimpleResult.ok(flashActivityDTO);
    }

    @Override
    public boolean isAllowPlaceOrderOrNot(Long activityId) {
        FlashActivityCache flashActivityCache = flashActivityCacheService.getCachedActivity(activityId, null);
        // 表示获得正在调整，不过应该避免在秒杀的过程中对活动进行调整。
        if (flashActivityCache.isLater()) {
            logger.info("isAllowPlaceOrderOrNot|稍后再试|{}", activityId);
            return false;
        }
        if (!flashActivityCache.isExist() || flashActivityCache.getFlashActivity() == null) {
            logger.info("isAllowPlaceOrderOrNot|活动不存在|{}", activityId);
            return false;
        }
        FlashActivity flashActivity = flashActivityCache.getFlashActivity();
        if (!flashActivity.isOnline()) {
            logger.info("isAllowPlaceOrderOrNot|活动尚未上线|{}", activityId);
            return false;
        }
        if (!flashActivity.isInProgress()) {
            logger.info("isAllowPlaceOrderOrNot|活动非秒杀时段|{}", activityId);
            return false;
        }
        // 可在此处丰富其他校验规则
        return true;
    }
}
