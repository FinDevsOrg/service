package ro.unibuc.prodeng.response;

import java.util.List;

public record WalletResponse(
    String id,
    String userId,
    List<AccountResponse> accounts
) {}
