package com.actionworks.flashsale.controller.resource;

import com.actionworks.flashsale.app.model.command.FlashActivityPublishCommand;
import com.actionworks.flashsale.app.model.dto.FlashActivityDTO;
import com.actionworks.flashsale.app.model.query.FlashActivitiesQuery;
import com.actionworks.flashsale.app.model.result.AppMultiResult;
import com.actionworks.flashsale.app.model.result.AppResult;
import com.actionworks.flashsale.app.model.result.AppSimpleResult;
import com.actionworks.flashsale.app.service.activity.FlashActivityAppService;
import com.actionworks.flashsale.controller.model.builder.FlashActivityBuilder;
import com.actionworks.flashsale.controller.model.builder.ResponseBuilder;
import com.actionworks.flashsale.controller.model.request.FlashActivityPublishRequest;
import com.actionworks.flashsale.controller.model.response.FlashActivityResponse;
import com.actionworks.flashsale.domain.model.enums.FlashActivityStatus;
import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

import static com.actionworks.flashsale.controller.model.builder.FlashActivityBuilder.toFlashActivitiesResponse;
import static com.actionworks.flashsale.controller.model.builder.FlashActivityBuilder.toFlashActivityResponse;

@RestController
public class FlashActivityController {

    @Resource
    private FlashActivityAppService flashActivityAppService;

    @PostMapping(value = "/flash-activities")
    public Response publishFlashActivity(@RequestAttribute Long userId, @RequestBody FlashActivityPublishRequest flashActivityPublishRequest) {
        FlashActivityPublishCommand activityPublishCommand = FlashActivityBuilder.toCommand(flashActivityPublishRequest);
        AppResult appResult = flashActivityAppService.publishFlashActivity(userId, activityPublishCommand);
        return ResponseBuilder.with(appResult);
    }

    @PutMapping(value = "/flash-activities/{activityId}")
    public Response modifyFlashActivity(@RequestAttribute Long userId, @PathVariable Long activityId, @RequestBody FlashActivityPublishRequest flashActivityPublishRequest) {
        FlashActivityPublishCommand activityPublishCommand = FlashActivityBuilder.toCommand(flashActivityPublishRequest);
        AppResult appResult = flashActivityAppService.modifyFlashActivity(userId, activityId, activityPublishCommand);
        return ResponseBuilder.with(appResult);
    }

    /**
     * todo: 注解：@RequestAttribute 是获取系统内容设置到 HttpServletRequest 的值，例如会在鉴权拦截器中将 token 转换为 userId，然后将 userId 设置到 RequestAttribute 中。
     * todo: 注解：@RequestParam 是获取前端传入的值，get 请求 ?pageSize=xx 和 post 请求 form 表单中的值
     *
     * @param userId
     * @param pageSize
     * @param pageNumber
     * @param keyword
     * @return
     */
    @GetMapping(value = "/flash-activities")
    @SentinelResource("GetActivitiesResource")
    public MultiResponse<FlashActivityResponse> getFlashActivities(@RequestAttribute Long userId,
                                                                   @RequestParam Integer pageSize,
                                                                   @RequestParam Integer pageNumber,
                                                                   @RequestParam(required = false) String keyword) {
        FlashActivitiesQuery flashActivitiesQuery = new FlashActivitiesQuery()
                .setKeyword(keyword)
                .setPageSize(pageSize)
                .setPageNumber(pageNumber);

        AppMultiResult<FlashActivityDTO> flashActivitiesResult = flashActivityAppService.getFlashActivities(userId, flashActivitiesQuery);
        return ResponseBuilder.withMulti(flashActivitiesResult);
    }

    @GetMapping(value = "/flash-activities/online")
    @SentinelResource("GetOnlineActivitiesResource")
    public MultiResponse<FlashActivityResponse> getOnlineFlashActivities(@RequestAttribute Long userId,
                                                                         @RequestParam Integer pageSize,
                                                                         @RequestParam Integer pageNumber,
                                                                         @RequestParam(required = false) String keyword) {
        FlashActivitiesQuery flashActivitiesQuery = new FlashActivitiesQuery()
                .setKeyword(keyword)
                .setPageSize(pageSize)
                .setPageNumber(pageNumber)
                .setStatus(FlashActivityStatus.ONLINE.getCode());

        AppMultiResult<FlashActivityDTO> flashActivitiesResult = flashActivityAppService.getFlashActivities(userId, flashActivitiesQuery);
        if (!flashActivitiesResult.isSuccess() || flashActivitiesResult.getData() == null) {
            return ResponseBuilder.withMulti(flashActivitiesResult);
        }
        return MultiResponse.of(toFlashActivitiesResponse(flashActivitiesResult.getData()), flashActivitiesResult.getTotal());
    }

    @GetMapping(value = "/flash-activities/{activityId}")
    @SentinelResource("GetActivityResource")
    public SingleResponse<FlashActivityResponse> getFlashActivity(@RequestAttribute Long userId,
                                                                  @PathVariable Long activityId,
                                                                  @RequestParam(required = false) Long version) {
        AppSimpleResult<FlashActivityDTO> flashActivityResult = flashActivityAppService.getFlashActivity(userId, activityId, version);
        if (!flashActivityResult.isSuccess() || flashActivityResult.getData() == null) {
            return ResponseBuilder.withSingle(flashActivityResult);
        }
        FlashActivityDTO flashActivityDTO = flashActivityResult.getData();
        return SingleResponse.of(toFlashActivityResponse(flashActivityDTO));
    }

    @PutMapping(value = "/flash-activities/{activityId}/online")
    public Response onlineFlashActivity(@RequestAttribute Long userId, @PathVariable Long activityId) {
        AppResult appResult = flashActivityAppService.onlineFlashActivity(userId, activityId);
        return ResponseBuilder.with(appResult);
    }

    @PutMapping(value = "/flash-activities/{activityId}/offline")
    public Response offlineFlashActivity(@RequestAttribute Long userId, @PathVariable Long activityId) {
        AppResult appResult = flashActivityAppService.offlineFlashActivity(userId, activityId);
        return ResponseBuilder.with(appResult);
    }
}
