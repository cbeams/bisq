/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.core.offer;

import bisq.core.account.witness.AccountAgeWitnessService;
import bisq.core.btc.wallet.BsqWalletService;
import bisq.core.btc.wallet.BtcWalletService;
import bisq.core.btc.wallet.Restrictions;
import bisq.core.filter.FilterManager;
import bisq.core.locale.CurrencyUtil;
import bisq.core.locale.Res;
import bisq.core.monetary.Price;
import bisq.core.payment.HalCashAccount;
import bisq.core.payment.PaymentAccount;
import bisq.core.payment.PaymentAccountUtil;
import bisq.core.provider.price.MarketPrice;
import bisq.core.provider.price.PriceFeedService;
import bisq.core.trade.statistics.ReferralIdService;
import bisq.core.user.Preferences;
import bisq.core.user.User;
import bisq.core.util.CoinUtil;

import bisq.network.p2p.NodeAddress;
import bisq.network.p2p.P2PService;

import bisq.common.app.Version;
import bisq.common.crypto.PubKeyRing;

import org.bitcoinj.core.Coin;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Singleton
public class CreateOfferService {
    private final PriceFeedService priceFeedService;
    private final BtcWalletService btcWalletService;
    private final BsqWalletService bsqWalletService;
    private final MakerFeeProvider makerFeeProvider;
    private final FilterManager filterManager;
    private final P2PService p2PService;
    private final PubKeyRing pubKeyRing;
    private final AccountAgeWitnessService accountAgeWitnessService;
    private final ReferralIdService referralIdService;
    private final User user;
    private final Preferences preferences;


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor, Initialization
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Inject
    public CreateOfferService(PriceFeedService priceFeedService,
                              BtcWalletService btcWalletService,
                              BsqWalletService bsqWalletService,
                              MakerFeeProvider makerFeeProvider,
                              FilterManager filterManager,
                              P2PService p2PService,
                              PubKeyRing pubKeyRing,
                              AccountAgeWitnessService accountAgeWitnessService,
                              ReferralIdService referralIdService,
                              User user,
                              Preferences preferences) {
        this.priceFeedService = priceFeedService;
        this.btcWalletService = btcWalletService;
        this.bsqWalletService = bsqWalletService;
        this.makerFeeProvider = makerFeeProvider;
        this.filterManager = filterManager;
        this.p2PService = p2PService;
        this.pubKeyRing = pubKeyRing;
        this.accountAgeWitnessService = accountAgeWitnessService;
        this.referralIdService = referralIdService;
        this.user = user;
        this.preferences = preferences;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////

    public Offer createAndGetOffer(String offerId,
                                   String currencyCode,
                                   OfferPayload.Direction direction,
                                   Price price,
                                   boolean useMarketBasedPrice,
                                   double marketPriceMargin,
                                   Coin amount,
                                   Coin minAmount,
                                   double buyerSecurityDeposit,
                                   double sellerSecurityDeposit,
                                   Coin txFeeFromFeeService,
                                   PaymentAccount paymentAccount) {
        boolean useMarketBasedPriceValue = useMarketBasedPriceValue(useMarketBasedPrice, currencyCode, paymentAccount);
        long priceAsLong = price != null && !useMarketBasedPriceValue ? price.getValue() : 0L;
        boolean isCryptoCurrency = CurrencyUtil.isCryptoCurrency(currencyCode);
        String baseCurrencyCode = isCryptoCurrency ? currencyCode : Res.getBaseCurrencyCode();
        String counterCurrencyCode = isCryptoCurrency ? Res.getBaseCurrencyCode() : currencyCode;

        double marketPriceMarginParam = useMarketBasedPriceValue ? marketPriceMargin : 0;
        long amountAsLong = amount != null ? amount.getValue() : 0L;
        long minAmountAsLong = minAmount != null ? minAmount.getValue() : 0L;

        List<String> acceptedCountryCodes = PaymentAccountUtil.getAcceptedCountryCodes(paymentAccount);
        List<String> acceptedBanks = PaymentAccountUtil.getAcceptedBanks(paymentAccount);
        String bankId = PaymentAccountUtil.getBankId(paymentAccount);
        String countryCode = PaymentAccountUtil.getCountryCode(paymentAccount);

        long maxTradeLimit = getMaxTradeLimit(paymentAccount, currencyCode, direction);
        long maxTradePeriod = paymentAccount.getMaxTradePeriod();

        // reserved for future use cases
        // Use null values if not set
        boolean isPrivateOffer = false;
        boolean useAutoClose = false;
        boolean useReOpenAfterAutoClose = false;
        long lowerClosePrice = 0;
        long upperClosePrice = 0;
        String hashOfChallenge = null;

        Coin makerFeeAsCoin = getMakerFee(amount);

        Map<String, String> extraDataMap = OfferUtil.getExtraDataMap(accountAgeWitnessService,
                referralIdService,
                paymentAccount,
                currencyCode,
                preferences);

        OfferUtil.validateOfferData(filterManager,
                p2PService,
                buyerSecurityDeposit,
                paymentAccount,
                currencyCode,
                makerFeeAsCoin);

        List<NodeAddress> acceptedArbitratorAddresses = user.getAcceptedArbitratorAddresses();
        ArrayList<NodeAddress> arbitratorNodeAddresses = acceptedArbitratorAddresses != null ?
                Lists.newArrayList(acceptedArbitratorAddresses) :
                new ArrayList<>();
        List<NodeAddress> acceptedMediatorAddresses = user.getAcceptedMediatorAddresses();
        ArrayList<NodeAddress> mediatorNodeAddresses = acceptedMediatorAddresses != null ?
                Lists.newArrayList(acceptedMediatorAddresses) :
                new ArrayList<>();

        OfferPayload offerPayload = new OfferPayload(offerId,
                new Date().getTime(),
                p2PService.getAddress(),
                pubKeyRing,
                OfferPayload.Direction.valueOf(direction.name()),
                priceAsLong,
                marketPriceMarginParam,
                useMarketBasedPriceValue,
                amountAsLong,
                minAmountAsLong,
                baseCurrencyCode,
                counterCurrencyCode,
                arbitratorNodeAddresses,
                mediatorNodeAddresses,
                paymentAccount.getPaymentMethod().getId(),
                paymentAccount.getId(),
                null,
                countryCode,
                acceptedCountryCodes,
                bankId,
                acceptedBanks,
                Version.VERSION,
                btcWalletService.getLastBlockSeenHeight(),
                txFeeFromFeeService.value,
                makerFeeAsCoin.value,
                isCurrencyForMakerFeeBtc(amount),
                getBuyerSecurityDepositAsCoin(amount, buyerSecurityDeposit).value,
                getSellerSecurityDepositAsCoin(amount, sellerSecurityDeposit).value,
                maxTradeLimit,
                maxTradePeriod,
                useAutoClose,
                useReOpenAfterAutoClose,
                upperClosePrice,
                lowerClosePrice,
                isPrivateOffer,
                hashOfChallenge,
                extraDataMap,
                Version.TRADE_PROTOCOL_VERSION);
        Offer offer = new Offer(offerPayload);
        offer.setPriceFeedService(priceFeedService);
        return offer;
    }

    private boolean useMarketBasedPriceValue(boolean useMarketBasedPrice,
                                             String currencyCode,
                                             PaymentAccount paymentAccount) {
        return useMarketBasedPrice &&
                isMarketPriceAvailable(currencyCode) &&
                !(paymentAccount instanceof HalCashAccount);
    }

    private boolean isMarketPriceAvailable(String currencyCode) {
        MarketPrice marketPrice = priceFeedService.getMarketPrice(currencyCode);
        return marketPrice != null && marketPrice.isExternallyProvidedPrice();
    }

    private Coin getMakerFee(Coin amount) {
        return makerFeeProvider.getMakerFee(bsqWalletService, preferences, amount);
    }

    private long getMaxTradeLimit(PaymentAccount paymentAccount,
                                  String currencyCode,
                                  OfferPayload.Direction direction) {
        if (paymentAccount != null) {
            return accountAgeWitnessService.getMyTradeLimit(paymentAccount, currencyCode, direction);
        } else {
            return 0;
        }
    }

    private boolean isCurrencyForMakerFeeBtc(Coin amount) {
        return OfferUtil.isCurrencyForMakerFeeBtc(preferences, bsqWalletService, amount);
    }

    private Coin getBuyerSecurityDepositAsCoin(Coin amount, double buyerSecurityDeposit) {
        Coin percentOfAmountAsCoin = CoinUtil.getPercentOfAmountAsCoin(buyerSecurityDeposit, amount);
        return getBoundedBuyerSecurityDepositAsCoin(percentOfAmountAsCoin);
    }

    private Coin getSellerSecurityDepositAsCoin(Coin amount, double sellerSecurityDeposit) {
        Coin amountAsCoin = amount;
        if (amountAsCoin == null)
            amountAsCoin = Coin.ZERO;

        Coin percentOfAmountAsCoin = CoinUtil.getPercentOfAmountAsCoin(sellerSecurityDeposit, amountAsCoin);
        return getBoundedSellerSecurityDepositAsCoin(percentOfAmountAsCoin);
    }

    private Coin getBoundedBuyerSecurityDepositAsCoin(Coin value) {
        // We need to ensure that for small amount values we don't get a too low BTC amount. We limit it with using the
        // MinBuyerSecurityDepositAsCoin from Restrictions.
        return Coin.valueOf(Math.max(Restrictions.getMinBuyerSecurityDepositAsCoin().value, value.value));
    }

    private Coin getBoundedSellerSecurityDepositAsCoin(Coin value) {
        // We need to ensure that for small amount values we don't get a too low BTC amount. We limit it with using the
        // MinSellerSecurityDepositAsCoin from Restrictions.
        return Coin.valueOf(Math.max(Restrictions.getMinSellerSecurityDepositAsCoin().value, value.value));
    }
}
