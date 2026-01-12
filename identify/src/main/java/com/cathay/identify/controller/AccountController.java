package com.cathay.identify.controller;

import com.cathay.identify.dto.request.account.AccountCreationRequest;
import com.cathay.identify.dto.request.account.AccountUpdateRequest;
import com.cathay.identify.dto.request.account.ChangePasswordRequest;
import com.cathay.identify.dto.response.ApiResponse;
import com.cathay.identify.entity.AccountEntity;
import com.cathay.identify.exception.AppException;
import com.cathay.identify.exception.ErrorCode;
import com.cathay.identify.service.AccountServiceImpl;
import com.cathay.identify.security.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {
    private final AccountServiceImpl accSer;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<AccountEntity> createAccount (@RequestBody @Valid AccountCreationRequest reqBody){
        return ApiResponse.<AccountEntity>builder()
                .result(accSer.createAccount(reqBody))
                .build();
    }

    @GetMapping
    public ApiResponse<List<AccountEntity>> getAccounts (){
        return ApiResponse.<List<AccountEntity>>builder()
                .result(accSer.getAccounts())
                .build();
    }

    @GetMapping("/{account_id}")
    public ApiResponse<AccountEntity> getAccountById(@PathVariable String account_id){
        val account = accSer.getAccountById(account_id);
        if (account.isPresent())
            return ApiResponse.<AccountEntity>builder()
                    .result(account.get())
                    .build();
        else throw new AppException(ErrorCode.USER_NOT_FOUND);
    }

    @GetMapping("/{email}")
    public ApiResponse<AccountEntity> getAccountByEmail(@PathVariable String email){
        val account = accSer.getAccountByEmail(email);
        if (account.isPresent())
            return ApiResponse.<AccountEntity>builder()
                    .result(account.get())
                    .build();
        else throw new AppException(ErrorCode.USER_NOT_FOUND);
    }

    @PutMapping("/{account_id}")
    public ApiResponse<AccountEntity> updateAccount(@PathVariable String account_id, @RequestBody AccountUpdateRequest updateDto){
        return ApiResponse.<AccountEntity>builder()
                .result(accSer.updateAccount(account_id, updateDto))
                .build();
    }

    @PostMapping("/change-password")
    public ApiResponse<AccountEntity> changePassword(@RequestBody String account_id, @RequestBody ChangePasswordRequest changePassDto){
        return ApiResponse.<AccountEntity>builder()
                .result(accSer.changePassword(account_id, changePassDto))
                .build();
    }

    /**
     * Example endpoint: Chỉ admin mới access được
     */
    @GetMapping("/admin-only")
    public ApiResponse<String> adminOnlyEndpoint(){
        if (!SecurityUtil.hasRole("ADMIN")) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return ApiResponse.<String>builder()
                .result("Welcome Admin! You have access to this endpoint.")
                .build();
    }

}
