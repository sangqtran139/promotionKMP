#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>
@class PromotionLogic__SkieTypeExportsKt, PromotionLogic__SkieSuspendWrappersKt, PromotionLogicVoucherTabItem, PromotionLogicVoucherTabInfoCompanion, PromotionLogicVoucherTabInfo, PromotionLogicVoucherStatusCompanion, PromotionLogicVoucherStatus, PromotionLogicVoucherMetadataDtoCompanion, PromotionLogicVoucherMetadataDto, PromotionLogicVoucherListItemCompanion, PromotionLogicVoucherListItem, PromotionLogicVoucherItem, PromotionLogicVoucherInfoDtoCompanion, PromotionLogicVoucherInfoDto, PromotionLogicVoucherDisplayState, PromotionLogicVoucherDetail, PromotionLogicVoucherCodeDtoCompanion, PromotionLogicVoucherCodeDto, PromotionLogicVoucherBrandDtoCompanion, PromotionLogicVoucherBrandDto, PromotionLogicValidationOptionsCompanion, PromotionLogicValidationOptions, PromotionLogicValidateStackableDiscountsUseCase, PromotionLogicValidateDiscountsResult, PromotionLogicValidateDiscountsRequest, PromotionLogicUShort, PromotionLogicULong, PromotionLogicUInt, PromotionLogicUByte, PromotionLogicStackingValidationResultCompanion, PromotionLogicStackingValidationResult, PromotionLogicStackingOptimizationCompanion, PromotionLogicStackingOptimization, PromotionLogicStackingExclusionCompanion, PromotionLogicStackingExclusion, PromotionLogicStackingConflictCompanion, PromotionLogicStackingConflict, PromotionLogicStackingAnalysisCompanion, PromotionLogicStackingAnalysis, PromotionLogicStackableOrderItemCompanion, PromotionLogicStackableOrderItem, PromotionLogicStackableOrderInfoCompanion, PromotionLogicStackableOrderInfo, PromotionLogicStackableGroupCompanion, PromotionLogicStackableGroup, PromotionLogicStackableDiscountsResponseCompanion, PromotionLogicStackableDiscountsResponse, PromotionLogicStackableDiscountsRequestCompanion, PromotionLogicStackableDiscountsRequest, PromotionLogicStackableCustomerInfoCompanion, PromotionLogicStackableCustomerInfo, PromotionLogicSortInfoCompanion, PromotionLogicSortInfo, PromotionLogicSkie_SuspendResultSuccess, PromotionLogicSkie_SuspendResultError, PromotionLogicSkie_SuspendResultCanceled, PromotionLogicSkie_SuspendResult, PromotionLogicSkie_SuspendHandler, PromotionLogicSkie_CancellationHandler, PromotionLogicSkieKotlinStateFlow<T>, PromotionLogicSkieKotlinSharedFlow<T>, PromotionLogicSkieKotlinOptionalStateFlow<T>, PromotionLogicSkieKotlinOptionalSharedFlow<T>, PromotionLogicSkieKotlinOptionalMutableStateFlow<T>, PromotionLogicSkieKotlinOptionalMutableSharedFlow<T>, PromotionLogicSkieKotlinOptionalFlow<T>, PromotionLogicSkieKotlinMutableStateFlow<T>, PromotionLogicSkieKotlinMutableSharedFlow<T>, PromotionLogicSkieKotlinFlow<T>, PromotionLogicSkieColdFlowIterator<E>, PromotionLogicShort, PromotionLogicSessionPreviewCompanion, PromotionLogicSessionPreview, PromotionLogicSessionOptionsCompanion, PromotionLogicSessionOptions, PromotionLogicServiceSelectorKt, PromotionLogicSearchMyPromotionStore, PromotionLogicSearchMyPromotionState, PromotionLogicSearchMyPromotionIntentSearch, PromotionLogicSearchMyPromotionIntentRetry, PromotionLogicSearchMyPromotionIntentQueryChanged, PromotionLogicSearchMyPromotionIntentLoadMore, PromotionLogicSearchMyPromotionIntentConsumeError, PromotionLogicSearchMyPromotionIntentClearKeyword, PromotionLogicSearchCustomerVouchersUseCase, PromotionLogicSearchCustomerVouchersResult, PromotionLogicSearchCustomerVouchersResponseCompanion, PromotionLogicSearchCustomerVouchersResponse, PromotionLogicSearchCustomerVouchersRequest, PromotionLogicSearchConfigKt, PromotionLogicSdkEnvironment, PromotionLogicResponseMetadataCompanion, PromotionLogicResponseMetadata, PromotionLogicRejectedOffer, PromotionLogicRedemptionValidationErrorResponseCompanion, PromotionLogicRedemptionValidationErrorResponse, PromotionLogicRedemptionValidationError, PromotionLogicRedemptionSessionResponseCompanion, PromotionLogicRedemptionSessionResponse, PromotionLogicRedemptionSessionRequestCompanion, PromotionLogicRedemptionSessionRequest, PromotionLogicRedemptionOrderItemCompanion, PromotionLogicRedemptionOrderItem, PromotionLogicRedemptionOrderInfoCompanion, PromotionLogicRedemptionOrderInfo, PromotionLogicRedemptionItemRequest, PromotionLogicRedemptionCustomerInfoCompanion, PromotionLogicRedemptionCustomerInfo, PromotionLogicRedeemableRequestCompanion, PromotionLogicRedeemableRequest, PromotionLogicPromotionUseCases, PromotionLogicPromotionSDKConfig, PromotionLogicPromotionResultSuccess<T>, PromotionLogicPromotionResultFailure, PromotionLogicPromotionPreferencesCompanion, PromotionLogicPromotionHtmlContentKt, PromotionLogicPromotionFeatureGate, PromotionLogicPromotionFeatureFlagsCompanion, PromotionLogicPromotionFeatureFlags, PromotionLogicPromotionFeatureFlagUseCases, PromotionLogicPromotionFeatureFlag, PromotionLogicPromotionException, PromotionLogicPromotionErrorCodes, PromotionLogicPromotionDetailStore, PromotionLogicPromotionDetailState, PromotionLogicPromotionDetailIntentLoadDetail, PromotionLogicPromotionDetailIntentConsumeError, PromotionLogicPromotionContainer, PromotionLogicPromotionCancellable, PromotionLogicPageableInfoCompanion, PromotionLogicPageableInfo, PromotionLogicPRMEffectShowError, PromotionLogicOfferWidgetStore, PromotionLogicOfferWidgetState, PromotionLogicOfferWidgetIntentValidateAndApply, PromotionLogicOfferWidgetIntentSetApplied, PromotionLogicOfferWidgetIntentMarkUnavailable, PromotionLogicOfferWidgetIntentLoadInitial, PromotionLogicOfferWidgetIntentConsumeError, PromotionLogicOfferWidgetIntentClearApplied, PromotionLogicOfferWidgetHostNotifier, PromotionLogicOfferWidgetHostEventVoucherApplied, PromotionLogicOfferWidgetDisplayState, PromotionLogicOfferWidgetConfirmResultSuccess, PromotionLogicOfferWidgetConfirmResultFailure, PromotionLogicOfferWidgetApplyOutcomeRejected, PromotionLogicOfferWidgetApplyOutcomeFailed, PromotionLogicOfferWidgetApplyOutcomeApplied, PromotionLogicOfferWidgetAppliedDiscount, PromotionLogicNumber, PromotionLogicNetworkException, PromotionLogicMyPromotionVoucher, PromotionLogicMyPromotionTab, PromotionLogicMyPromotionStore, PromotionLogicMyPromotionState, PromotionLogicMyPromotionIntentSelectTab, PromotionLogicMyPromotionIntentSearch, PromotionLogicMyPromotionIntentRefresh, PromotionLogicMyPromotionIntentLoadMore, PromotionLogicMyPromotionIntentLoadInitialIfNeeded, PromotionLogicMyPromotionIntentConsumeError, PromotionLogicMyPromotionBadge, PromotionLogicMyPromotionAction, PromotionLogicMutableSet<ObjectType>, PromotionLogicMutableDictionary<KeyType, ObjectType>, PromotionLogicLong, PromotionLogicKotlinx_serialization_jsonJsonPrimitiveCompanion, PromotionLogicKotlinx_serialization_jsonJsonPrimitive, PromotionLogicKotlinx_serialization_jsonJsonNull, PromotionLogicKotlinx_serialization_jsonJsonElementCompanion, PromotionLogicKotlinx_serialization_jsonJsonElement, PromotionLogicKotlinx_serialization_coreStructureKindOBJECT, PromotionLogicKotlinx_serialization_coreStructureKindMAP, PromotionLogicKotlinx_serialization_coreStructureKindLIST, PromotionLogicKotlinx_serialization_coreStructureKindCLASS, PromotionLogicKotlinx_serialization_coreStructureKind, PromotionLogicKotlinx_serialization_coreSerializersModule, PromotionLogicKotlinx_serialization_coreSerialKindENUM, PromotionLogicKotlinx_serialization_coreSerialKindCONTEXTUAL, PromotionLogicKotlinx_serialization_coreSerialKind, PromotionLogicKotlinx_serialization_corePrimitiveKindSTRING, PromotionLogicKotlinx_serialization_corePrimitiveKindSHORT, PromotionLogicKotlinx_serialization_corePrimitiveKindLONG, PromotionLogicKotlinx_serialization_corePrimitiveKindINT, PromotionLogicKotlinx_serialization_corePrimitiveKindFLOAT, PromotionLogicKotlinx_serialization_corePrimitiveKindDOUBLE, PromotionLogicKotlinx_serialization_corePrimitiveKindCHAR, PromotionLogicKotlinx_serialization_corePrimitiveKindBYTE, PromotionLogicKotlinx_serialization_corePrimitiveKindBOOLEAN, PromotionLogicKotlinx_serialization_corePrimitiveKind, PromotionLogicKotlinx_serialization_corePolymorphicKindSEALED, PromotionLogicKotlinx_serialization_corePolymorphicKindOPEN, PromotionLogicKotlinx_serialization_corePolymorphicKind, PromotionLogicKotlinThrowable, PromotionLogicKotlinRuntimeException, PromotionLogicKotlinNothing, PromotionLogicKotlinIllegalStateException, PromotionLogicKotlinException, PromotionLogicKotlinEnumCompanion, PromotionLogicKotlinEnum<E>, PromotionLogicKotlinCancellationException, PromotionLogicKotlinArray<T>, PromotionLogicIsFeatureEnabledUseCase, PromotionLogicInt, PromotionLogicGetPromotionFeatureFlagsUseCase, PromotionLogicGetFeatureFlagsUseCase, PromotionLogicGetCustomerVoucherDetailUseCase, PromotionLogicFloat, PromotionLogicFindEligibleCampaignsUseCase, PromotionLogicFindEligibleCampaignsRequest, PromotionLogicFetchFeatureFlagsUseCase, PromotionLogicFeatureFlagException, PromotionLogicFeatureFlag, PromotionLogicEmptyPromotionRequestContextProvider, PromotionLogicEligibleSection, PromotionLogicEligibleOrderItem, PromotionLogicEligibleOffersResult, PromotionLogicEligibleOffer, PromotionLogicEligibleFilterOptions, PromotionLogicDouble, PromotionLogicDiscountRequestCompanion, PromotionLogicDiscountRequest, PromotionLogicDiscountItemResult, PromotionLogicDiscountItemRequest, PromotionLogicDiscountDetailCompanion, PromotionLogicDiscountDetail, PromotionLogicCustomerVoucherDetailCompanion, PromotionLogicCustomerVoucherDetail, PromotionLogicCreateRedemptionSessionUseCase, PromotionLogicCreateRedemptionResult, PromotionLogicCreateRedemptionRequest, PromotionLogicChooseSeeMoreState, PromotionLogicChoosePromotionStore, PromotionLogicChoosePromotionState, PromotionLogicChoosePromotionIntentToggleSelection, PromotionLogicChoosePromotionIntentSetPreSelected, PromotionLogicChoosePromotionIntentSeedOnce, PromotionLogicChoosePromotionIntentSeeMoreMy, PromotionLogicChoosePromotionIntentSearch, PromotionLogicChoosePromotionIntentRefresh, PromotionLogicChoosePromotionIntentQueryChanged, PromotionLogicChoosePromotionIntentPreload, PromotionLogicChoosePromotionIntentLoadMoreOtherVouchers, PromotionLogicChoosePromotionIntentLoadMoreMyVouchers, PromotionLogicChoosePromotionIntentLoadInitial, PromotionLogicChoosePromotionIntentConsumeError, PromotionLogicChoosePromotionIntentConsumeApplyMessage, PromotionLogicChoosePromotionIntentClearKeyword, PromotionLogicChoosePromotionIntentApplyStarted, PromotionLogicChoosePromotionIntentApplyRejected, PromotionLogicChoosePromotionIntentApplyFinished, PromotionLogicChoosePromotionContractKt, PromotionLogicChooseOffer, PromotionLogicByte, PromotionLogicBusinessRuleViolationCompanion, PromotionLogicBusinessRuleViolation, PromotionLogicBudgetHoldCompanion, PromotionLogicBudgetHold, PromotionLogicBoolean, PromotionLogicBase, PromotionLogicAvailableService, PromotionLogicAppliedDiscountCompanion, PromotionLogicAppliedDiscount, PromotionLogicApplicableProductDtoCompanion, PromotionLogicApplicableProductDto, PromotionLogicApplicableProduct, PromotionLogicApiResponseTemplateCompanion, PromotionLogicApiResponseTemplate<T>, NSString, NSSet<ObjectType>, NSObject, NSNumber, NSMutableSet<ObjectType>, NSMutableDictionary<KeyType, ObjectType>, NSMutableArray<ObjectType>, NSError, NSDictionary<KeyType, ObjectType>, NSArray<ObjectType>;
@protocol PromotionLogicSkie_DispatcherDelegate, PromotionLogicSearchMyPromotionIntent, PromotionLogicPromotionResult, PromotionLogicPromotionRequestContextProvider, PromotionLogicPromotionPreferences, PromotionLogicPromotionDetailIntent, PromotionLogicPRMStore, PromotionLogicPRMEffect, PromotionLogicOfferWidgetIntent, PromotionLogicOfferWidgetHostEvent, PromotionLogicOfferWidgetConfirmResult, PromotionLogicOfferWidgetApplyOutcome, PromotionLogicMyPromotionIntent, PromotionLogicKotlinx_serialization_coreSerializersModuleCollector, PromotionLogicKotlinx_serialization_coreSerializationStrategy, PromotionLogicKotlinx_serialization_coreSerialDescriptor, PromotionLogicKotlinx_serialization_coreKSerializer, PromotionLogicKotlinx_serialization_coreEncoder, PromotionLogicKotlinx_serialization_coreDeserializationStrategy, PromotionLogicKotlinx_serialization_coreDecoder, PromotionLogicKotlinx_serialization_coreCompositeEncoder, PromotionLogicKotlinx_serialization_coreCompositeDecoder, PromotionLogicKotlinx_coroutines_coreStateFlow, PromotionLogicKotlinx_coroutines_coreSharedFlow, PromotionLogicKotlinx_coroutines_coreRunnable, PromotionLogicKotlinx_coroutines_coreMutableStateFlow, PromotionLogicKotlinx_coroutines_coreMutableSharedFlow, PromotionLogicKotlinx_coroutines_coreFlowCollector, PromotionLogicKotlinx_coroutines_coreFlow, PromotionLogicKotlinx_coroutines_coreCoroutineScope, PromotionLogicKotlinKDeclarationContainer, PromotionLogicKotlinKClassifier, PromotionLogicKotlinKClass, PromotionLogicKotlinKAnnotatedElement, PromotionLogicKotlinIterator, PromotionLogicKotlinCoroutineContextKey, PromotionLogicKotlinCoroutineContextElement, PromotionLogicKotlinCoroutineContext, PromotionLogicKotlinComparable, PromotionLogicKotlinAnnotation, PromotionLogicChoosePromotionIntent, NSCopying;
@interface __SkieLambdaErrorType : NSObject
- (instancetype _Nonnull)init __attribute__((unavailable));
+ (instancetype _Nonnull)new __attribute__((unavailable));
@end
@interface __SkieUnknownCInteropFrameworkErrorType : NSObject
- (instancetype _Nonnull)init __attribute__((unavailable));
+ (instancetype _Nonnull)new __attribute__((unavailable));
@end
NS_ASSUME_NONNULL_BEGIN
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunknown-warning-option"
#pragma clang diagnostic ignored "-Wincompatible-property-type"
#pragma clang diagnostic ignored "-Wnullability"
#pragma push_macro("_Nullable_result")
#if !__has_feature(nullability_nullable_result)
#undef _Nullable_result
#define _Nullable_result _Nullable
#endif
__attribute__((swift_name("KotlinBase")))
@interface PromotionLogicBase : NSObject
- (instancetype)init __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (void)initialize __attribute__((objc_requires_super));
@end
@interface PromotionLogicBase (PromotionLogicBaseCopying) <NSCopying>
@end
__attribute__((swift_name("KotlinMutableSet")))
@interface PromotionLogicMutableSet<ObjectType> : NSMutableSet<ObjectType>
@end
__attribute__((swift_name("KotlinMutableDictionary")))
@interface PromotionLogicMutableDictionary<KeyType, ObjectType> : NSMutableDictionary<KeyType, ObjectType>
@end
@interface NSError (NSErrorPromotionLogicKotlinException)
@property (readonly) id _Nullable kotlinException;
@end
__attribute__((swift_name("KotlinNumber")))
@interface PromotionLogicNumber : NSNumber
- (instancetype)initWithChar:(char)value __attribute__((unavailable));
- (instancetype)initWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
- (instancetype)initWithShort:(short)value __attribute__((unavailable));
- (instancetype)initWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
- (instancetype)initWithInt:(int)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
- (instancetype)initWithLong:(long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
- (instancetype)initWithLongLong:(long long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
- (instancetype)initWithFloat:(float)value __attribute__((unavailable));
- (instancetype)initWithDouble:(double)value __attribute__((unavailable));
- (instancetype)initWithBool:(BOOL)value __attribute__((unavailable));
- (instancetype)initWithInteger:(NSInteger)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
+ (instancetype)numberWithChar:(char)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
+ (instancetype)numberWithShort:(short)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
+ (instancetype)numberWithInt:(int)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
+ (instancetype)numberWithLong:(long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
+ (instancetype)numberWithLongLong:(long long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
+ (instancetype)numberWithFloat:(float)value __attribute__((unavailable));
+ (instancetype)numberWithDouble:(double)value __attribute__((unavailable));
+ (instancetype)numberWithBool:(BOOL)value __attribute__((unavailable));
+ (instancetype)numberWithInteger:(NSInteger)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
@end
__attribute__((swift_name("KotlinByte")))
@interface PromotionLogicByte : PromotionLogicNumber
- (instancetype)initWithChar:(char)value;
+ (instancetype)numberWithChar:(char)value;
@end
__attribute__((swift_name("KotlinUByte")))
@interface PromotionLogicUByte : PromotionLogicNumber
- (instancetype)initWithUnsignedChar:(unsigned char)value;
+ (instancetype)numberWithUnsignedChar:(unsigned char)value;
@end
__attribute__((swift_name("KotlinShort")))
@interface PromotionLogicShort : PromotionLogicNumber
- (instancetype)initWithShort:(short)value;
+ (instancetype)numberWithShort:(short)value;
@end
__attribute__((swift_name("KotlinUShort")))
@interface PromotionLogicUShort : PromotionLogicNumber
- (instancetype)initWithUnsignedShort:(unsigned short)value;
+ (instancetype)numberWithUnsignedShort:(unsigned short)value;
@end
__attribute__((swift_name("KotlinInt")))
@interface PromotionLogicInt : PromotionLogicNumber
- (instancetype)initWithInt:(int)value;
+ (instancetype)numberWithInt:(int)value;
@end
__attribute__((swift_name("KotlinUInt")))
@interface PromotionLogicUInt : PromotionLogicNumber
- (instancetype)initWithUnsignedInt:(unsigned int)value;
+ (instancetype)numberWithUnsignedInt:(unsigned int)value;
@end
__attribute__((swift_name("KotlinLong")))
@interface PromotionLogicLong : PromotionLogicNumber
- (instancetype)initWithLongLong:(long long)value;
+ (instancetype)numberWithLongLong:(long long)value;
@end
__attribute__((swift_name("KotlinULong")))
@interface PromotionLogicULong : PromotionLogicNumber
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value;
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value;
@end
__attribute__((swift_name("KotlinFloat")))
@interface PromotionLogicFloat : PromotionLogicNumber
- (instancetype)initWithFloat:(float)value;
+ (instancetype)numberWithFloat:(float)value;
@end
__attribute__((swift_name("KotlinDouble")))
@interface PromotionLogicDouble : PromotionLogicNumber
- (instancetype)initWithDouble:(double)value;
+ (instancetype)numberWithDouble:(double)value;
@end
__attribute__((swift_name("KotlinBoolean")))
@interface PromotionLogicBoolean : PromotionLogicNumber
- (instancetype)initWithBool:(BOOL)value;
+ (instancetype)numberWithBool:(BOOL)value;
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkieColdFlowIterator")))
@interface PromotionLogicSkieColdFlowIterator<E> : PromotionLogicBase
- (instancetype)initWithFlow:(id<PromotionLogicKotlinx_coroutines_coreFlow>)flow __attribute__((swift_name("init(flow:)"))) __attribute__((objc_designated_initializer));
- (void)cancel __attribute__((swift_name("cancel()")));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)hasNextWithCompletionHandler:(void (^)(PromotionLogicBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("hasNext(completionHandler:)")));
- (E _Nullable)next __attribute__((swift_name("next()")));
@end
__attribute__((swift_name("Kotlinx_coroutines_coreFlow")))
@protocol PromotionLogicKotlinx_coroutines_coreFlow
@required
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)collectCollector:(id<PromotionLogicKotlinx_coroutines_coreFlowCollector>)collector completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collect(collector:completionHandler:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkieKotlinFlow")))
@interface PromotionLogicSkieKotlinFlow<__covariant T> : PromotionLogicBase <PromotionLogicKotlinx_coroutines_coreFlow>
- (instancetype)initWithDelegate:(id<PromotionLogicKotlinx_coroutines_coreFlow>)delegate __attribute__((swift_name("init(_:)"))) __attribute__((objc_designated_initializer));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)collectCollector:(id<PromotionLogicKotlinx_coroutines_coreFlowCollector>)collector completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collect(collector:completionHandler:)")));
@end
__attribute__((swift_name("Kotlinx_coroutines_coreSharedFlow")))
@protocol PromotionLogicKotlinx_coroutines_coreSharedFlow <PromotionLogicKotlinx_coroutines_coreFlow>
@required
@property (readonly) NSArray<id> *replayCache __attribute__((swift_name("replayCache")));
@end
__attribute__((swift_name("Kotlinx_coroutines_coreFlowCollector")))
@protocol PromotionLogicKotlinx_coroutines_coreFlowCollector
@required
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)emitValue:(id _Nullable)value completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("emit(value:completionHandler:)")));
@end
__attribute__((swift_name("Kotlinx_coroutines_coreMutableSharedFlow")))
@protocol PromotionLogicKotlinx_coroutines_coreMutableSharedFlow <PromotionLogicKotlinx_coroutines_coreSharedFlow, PromotionLogicKotlinx_coroutines_coreFlowCollector>
@required
/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
- (void)resetReplayCache __attribute__((swift_name("resetReplayCache()")));
- (BOOL)tryEmitValue:(id _Nullable)value __attribute__((swift_name("tryEmit(value:)")));
@property (readonly) id<PromotionLogicKotlinx_coroutines_coreStateFlow> subscriptionCount __attribute__((swift_name("subscriptionCount")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkieKotlinMutableSharedFlow")))
@interface PromotionLogicSkieKotlinMutableSharedFlow<T> : PromotionLogicBase <PromotionLogicKotlinx_coroutines_coreMutableSharedFlow>
@property (readonly) NSArray<T> *replayCache __attribute__((swift_name("replayCache")));
@property (readonly) id<PromotionLogicKotlinx_coroutines_coreStateFlow> subscriptionCount __attribute__((swift_name("subscriptionCount")));
- (instancetype)initWithDelegate:(id<PromotionLogicKotlinx_coroutines_coreMutableSharedFlow>)delegate __attribute__((swift_name("init(_:)"))) __attribute__((objc_designated_initializer));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)collectCollector:(id<PromotionLogicKotlinx_coroutines_coreFlowCollector>)collector completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collect(collector:completionHandler:)")));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)emitValue:(T)value completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("emit(value:completionHandler:)")));
/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
- (void)resetReplayCache __attribute__((swift_name("resetReplayCache()")));
- (BOOL)tryEmitValue:(T)value __attribute__((swift_name("tryEmit(value:)")));
@end
__attribute__((swift_name("Kotlinx_coroutines_coreStateFlow")))
@protocol PromotionLogicKotlinx_coroutines_coreStateFlow <PromotionLogicKotlinx_coroutines_coreSharedFlow>
@required
@property (readonly) id _Nullable value __attribute__((swift_name("value")));
@end
__attribute__((swift_name("Kotlinx_coroutines_coreMutableStateFlow")))
@protocol PromotionLogicKotlinx_coroutines_coreMutableStateFlow <PromotionLogicKotlinx_coroutines_coreStateFlow, PromotionLogicKotlinx_coroutines_coreMutableSharedFlow>
@required
- (void)setValue:(id _Nullable)value __attribute__((swift_name("setValue(_:)")));
- (BOOL)compareAndSetExpect:(id _Nullable)expect update:(id _Nullable)update __attribute__((swift_name("compareAndSet(expect:update:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkieKotlinMutableStateFlow")))
@interface PromotionLogicSkieKotlinMutableStateFlow<T> : PromotionLogicBase <PromotionLogicKotlinx_coroutines_coreMutableStateFlow>
@property (readonly) NSArray<T> *replayCache __attribute__((swift_name("replayCache")));
@property (readonly) id<PromotionLogicKotlinx_coroutines_coreStateFlow> subscriptionCount __attribute__((swift_name("subscriptionCount")));
@property T value __attribute__((swift_name("value")));
- (instancetype)initWithDelegate:(id<PromotionLogicKotlinx_coroutines_coreMutableStateFlow>)delegate __attribute__((swift_name("init(_:)"))) __attribute__((objc_designated_initializer));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)collectCollector:(id<PromotionLogicKotlinx_coroutines_coreFlowCollector>)collector completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collect(collector:completionHandler:)")));
- (BOOL)compareAndSetExpect:(T)expect update:(T)update __attribute__((swift_name("compareAndSet(expect:update:)")));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)emitValue:(T)value completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("emit(value:completionHandler:)")));
/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
- (void)resetReplayCache __attribute__((swift_name("resetReplayCache()")));
- (BOOL)tryEmitValue:(T)value __attribute__((swift_name("tryEmit(value:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkieKotlinOptionalFlow")))
@interface PromotionLogicSkieKotlinOptionalFlow<__covariant T> : PromotionLogicBase <PromotionLogicKotlinx_coroutines_coreFlow>
- (instancetype)initWithDelegate:(id<PromotionLogicKotlinx_coroutines_coreFlow>)delegate __attribute__((swift_name("init(_:)"))) __attribute__((objc_designated_initializer));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)collectCollector:(id<PromotionLogicKotlinx_coroutines_coreFlowCollector>)collector completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collect(collector:completionHandler:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkieKotlinOptionalMutableSharedFlow")))
@interface PromotionLogicSkieKotlinOptionalMutableSharedFlow<T> : PromotionLogicBase <PromotionLogicKotlinx_coroutines_coreMutableSharedFlow>
@property (readonly) NSArray<id> *replayCache __attribute__((swift_name("replayCache")));
@property (readonly) id<PromotionLogicKotlinx_coroutines_coreStateFlow> subscriptionCount __attribute__((swift_name("subscriptionCount")));
- (instancetype)initWithDelegate:(id<PromotionLogicKotlinx_coroutines_coreMutableSharedFlow>)delegate __attribute__((swift_name("init(_:)"))) __attribute__((objc_designated_initializer));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)collectCollector:(id<PromotionLogicKotlinx_coroutines_coreFlowCollector>)collector completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collect(collector:completionHandler:)")));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)emitValue:(T _Nullable)value completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("emit(value:completionHandler:)")));
/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
- (void)resetReplayCache __attribute__((swift_name("resetReplayCache()")));
- (BOOL)tryEmitValue:(T _Nullable)value __attribute__((swift_name("tryEmit(value:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkieKotlinOptionalMutableStateFlow")))
@interface PromotionLogicSkieKotlinOptionalMutableStateFlow<T> : PromotionLogicBase <PromotionLogicKotlinx_coroutines_coreMutableStateFlow>
@property (readonly) NSArray<id> *replayCache __attribute__((swift_name("replayCache")));
@property (readonly) id<PromotionLogicKotlinx_coroutines_coreStateFlow> subscriptionCount __attribute__((swift_name("subscriptionCount")));
@property T _Nullable value __attribute__((swift_name("value")));
- (instancetype)initWithDelegate:(id<PromotionLogicKotlinx_coroutines_coreMutableStateFlow>)delegate __attribute__((swift_name("init(_:)"))) __attribute__((objc_designated_initializer));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)collectCollector:(id<PromotionLogicKotlinx_coroutines_coreFlowCollector>)collector completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collect(collector:completionHandler:)")));
- (BOOL)compareAndSetExpect:(T _Nullable)expect update:(T _Nullable)update __attribute__((swift_name("compareAndSet(expect:update:)")));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)emitValue:(T _Nullable)value completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("emit(value:completionHandler:)")));
/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
- (void)resetReplayCache __attribute__((swift_name("resetReplayCache()")));
- (BOOL)tryEmitValue:(T _Nullable)value __attribute__((swift_name("tryEmit(value:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkieKotlinOptionalSharedFlow")))
@interface PromotionLogicSkieKotlinOptionalSharedFlow<__covariant T> : PromotionLogicBase <PromotionLogicKotlinx_coroutines_coreSharedFlow>
@property (readonly) NSArray<id> *replayCache __attribute__((swift_name("replayCache")));
- (instancetype)initWithDelegate:(id<PromotionLogicKotlinx_coroutines_coreSharedFlow>)delegate __attribute__((swift_name("init(_:)"))) __attribute__((objc_designated_initializer));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)collectCollector:(id<PromotionLogicKotlinx_coroutines_coreFlowCollector>)collector completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collect(collector:completionHandler:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkieKotlinOptionalStateFlow")))
@interface PromotionLogicSkieKotlinOptionalStateFlow<__covariant T> : PromotionLogicBase <PromotionLogicKotlinx_coroutines_coreStateFlow>
@property (readonly) NSArray<id> *replayCache __attribute__((swift_name("replayCache")));
@property (readonly) T _Nullable value __attribute__((swift_name("value")));
- (instancetype)initWithDelegate:(id<PromotionLogicKotlinx_coroutines_coreStateFlow>)delegate __attribute__((swift_name("init(_:)"))) __attribute__((objc_designated_initializer));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)collectCollector:(id<PromotionLogicKotlinx_coroutines_coreFlowCollector>)collector completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collect(collector:completionHandler:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkieKotlinSharedFlow")))
@interface PromotionLogicSkieKotlinSharedFlow<__covariant T> : PromotionLogicBase <PromotionLogicKotlinx_coroutines_coreSharedFlow>
@property (readonly) NSArray<T> *replayCache __attribute__((swift_name("replayCache")));
- (instancetype)initWithDelegate:(id<PromotionLogicKotlinx_coroutines_coreSharedFlow>)delegate __attribute__((swift_name("init(_:)"))) __attribute__((objc_designated_initializer));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)collectCollector:(id<PromotionLogicKotlinx_coroutines_coreFlowCollector>)collector completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collect(collector:completionHandler:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SkieKotlinStateFlow")))
@interface PromotionLogicSkieKotlinStateFlow<__covariant T> : PromotionLogicBase <PromotionLogicKotlinx_coroutines_coreStateFlow>
@property (readonly) NSArray<T> *replayCache __attribute__((swift_name("replayCache")));
@property (readonly) T value __attribute__((swift_name("value")));
- (instancetype)initWithDelegate:(id<PromotionLogicKotlinx_coroutines_coreStateFlow>)delegate __attribute__((swift_name("init(_:)"))) __attribute__((objc_designated_initializer));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)collectCollector:(id<PromotionLogicKotlinx_coroutines_coreFlowCollector>)collector completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collect(collector:completionHandler:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Skie_CancellationHandler")))
@interface PromotionLogicSkie_CancellationHandler : PromotionLogicBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)cancel __attribute__((swift_name("cancel()")));
@end
__attribute__((swift_name("Skie_DispatcherDelegate")))
@protocol PromotionLogicSkie_DispatcherDelegate
@required
- (void)dispatchBlock:(id<PromotionLogicKotlinx_coroutines_coreRunnable>)block __attribute__((swift_name("dispatch(block:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Skie_SuspendHandler")))
@interface PromotionLogicSkie_SuspendHandler : PromotionLogicBase
- (instancetype)initWithCancellationHandler:(PromotionLogicSkie_CancellationHandler *)cancellationHandler dispatcherDelegate:(id<PromotionLogicSkie_DispatcherDelegate>)dispatcherDelegate onResult:(void (^)(PromotionLogicSkie_SuspendResult *))onResult __attribute__((swift_name("init(cancellationHandler:dispatcherDelegate:onResult:)"))) __attribute__((objc_designated_initializer));
@end
__attribute__((swift_name("Skie_SuspendResult")))
@interface PromotionLogicSkie_SuspendResult : PromotionLogicBase
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Skie_SuspendResult.Canceled")))
@interface PromotionLogicSkie_SuspendResultCanceled : PromotionLogicSkie_SuspendResult
@property (class, readonly, getter=shared) PromotionLogicSkie_SuspendResultCanceled *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)canceled __attribute__((swift_name("init()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Skie_SuspendResult.Error")))
@interface PromotionLogicSkie_SuspendResultError : PromotionLogicSkie_SuspendResult
@property (readonly) NSError *error __attribute__((swift_name("error")));
- (instancetype)initWithError:(NSError *)error __attribute__((swift_name("init(error:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicSkie_SuspendResultError *)doCopyError:(NSError *)error __attribute__((swift_name("doCopy(error:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Skie_SuspendResult.Success")))
@interface PromotionLogicSkie_SuspendResultSuccess : PromotionLogicSkie_SuspendResult
@property (readonly) id _Nullable value __attribute__((swift_name("value")));
- (instancetype)initWithValue:(id _Nullable)value __attribute__((swift_name("init(value:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicSkie_SuspendResultSuccess *)doCopyValue:(id _Nullable)value __attribute__((swift_name("doCopy(value:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AvailableService")))
@interface PromotionLogicAvailableService : PromotionLogicBase
@property (readonly) NSString *iconUrl __attribute__((swift_name("iconUrl")));
@property (readonly) NSString *productId __attribute__((swift_name("productId")));
@property (readonly) NSString *productName __attribute__((swift_name("productName")));
@property (readonly) NSString *skuSourceId __attribute__((swift_name("skuSourceId")));
- (instancetype)initWithProductId:(NSString *)productId productName:(NSString *)productName skuSourceId:(NSString *)skuSourceId iconUrl:(NSString *)iconUrl __attribute__((swift_name("init(productId:productName:skuSourceId:iconUrl:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicAvailableService *)doCopyProductId:(NSString *)productId productName:(NSString *)productName skuSourceId:(NSString *)skuSourceId iconUrl:(NSString *)iconUrl __attribute__((swift_name("doCopy(productId:productName:skuSourceId:iconUrl:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((swift_name("PromotionRequestContextProvider")))
@protocol PromotionLogicPromotionRequestContextProvider
@required
- (NSString * _Nullable)getAccessToken __attribute__((swift_name("getAccessToken()")));
- (NSString * _Nullable)getLanguage __attribute__((swift_name("getLanguage()")));
- (NSString * _Nullable)getMetaData __attribute__((swift_name("getMetaData()")));
- (NSString * _Nullable)getOrderId __attribute__((swift_name("getOrderId()")));
- (NSArray<PromotionLogicEligibleOrderItem *> *)getOrderItems __attribute__((swift_name("getOrderItems()")));
- (NSString * _Nullable)getOrderValue __attribute__((swift_name("getOrderValue()")));
- (NSString * _Nullable)getService __attribute__((swift_name("getService()")));
- (void)refreshAccessTokenOnResult:(void (^)(PromotionLogicBoolean *))onResult __attribute__((swift_name("refreshAccessToken(onResult:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EmptyPromotionRequestContextProvider")))
@interface PromotionLogicEmptyPromotionRequestContextProvider : PromotionLogicBase <PromotionLogicPromotionRequestContextProvider>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PromotionSDKConfig")))
@interface PromotionLogicPromotionSDKConfig : PromotionLogicBase
@property (readonly) NSArray<PromotionLogicAvailableService *> *availableServices __attribute__((swift_name("availableServices")));
@property (readonly) NSString *baseUrl __attribute__((swift_name("baseUrl")));
@property (readonly) PromotionLogicSdkEnvironment *environment __attribute__((swift_name("environment")));
@property (readonly) BOOL isDebug __attribute__((swift_name("isDebug")));
@property (readonly) id<PromotionLogicPromotionRequestContextProvider> _Nullable requestContextProvider __attribute__((swift_name("requestContextProvider")));
- (instancetype)initWithBaseUrl:(NSString *)baseUrl requestContextProvider:(id<PromotionLogicPromotionRequestContextProvider> _Nullable)requestContextProvider environment:(PromotionLogicSdkEnvironment *)environment availableServices:(NSArray<PromotionLogicAvailableService *> *)availableServices isDebug:(BOOL)isDebug __attribute__((swift_name("init(baseUrl:requestContextProvider:environment:availableServices:isDebug:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicPromotionSDKConfig *)doCopyBaseUrl:(NSString *)baseUrl requestContextProvider:(id<PromotionLogicPromotionRequestContextProvider> _Nullable)requestContextProvider environment:(PromotionLogicSdkEnvironment *)environment availableServices:(NSArray<PromotionLogicAvailableService *> *)availableServices isDebug:(BOOL)isDebug __attribute__((swift_name("doCopy(baseUrl:requestContextProvider:environment:availableServices:isDebug:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((swift_name("KotlinComparable")))
@protocol PromotionLogicKotlinComparable
@required
- (int32_t)compareToOther:(id _Nullable)other __attribute__((swift_name("compareTo(other:)")));
@end
__attribute__((swift_name("KotlinEnum")))
@interface PromotionLogicKotlinEnum<E> : PromotionLogicBase <PromotionLogicKotlinComparable>
@property (class, readonly, getter=companion) PromotionLogicKotlinEnumCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) int32_t ordinal __attribute__((swift_name("ordinal")));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer));
- (int32_t)compareToOther:(E)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SdkEnvironment")))
@interface PromotionLogicSdkEnvironment : PromotionLogicKotlinEnum<PromotionLogicSdkEnvironment *>
@property (class, readonly) PromotionLogicSdkEnvironment *prod __attribute__((swift_name("prod")));
@property (class, readonly) PromotionLogicSdkEnvironment *staging __attribute__((swift_name("staging")));
@property (class, readonly) NSArray<PromotionLogicSdkEnvironment *> *entries __attribute__((swift_name("entries")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (PromotionLogicKotlinArray<PromotionLogicSdkEnvironment *> *)values __attribute__((swift_name("values()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AppliedDiscount")))
@interface PromotionLogicAppliedDiscount : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicAppliedDiscountCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString *appliedTo __attribute__((swift_name("appliedTo")));
@property (readonly) NSString *discountAmount __attribute__((swift_name("discountAmount")));
@property (readonly) NSString *discountPercentage __attribute__((swift_name("discountPercentage")));
@property (readonly) NSString *discountType __attribute__((swift_name("discountType")));
@property (readonly) int32_t priority __attribute__((swift_name("priority")));
@property (readonly) NSString *redeemableId __attribute__((swift_name("redeemableId")));
@property (readonly) NSString *redeemableName __attribute__((swift_name("redeemableName")));
@property (readonly) NSString *redeemableType __attribute__((swift_name("redeemableType")));
- (instancetype)initWithRedeemableId:(NSString *)redeemableId redeemableType:(NSString *)redeemableType redeemableName:(NSString *)redeemableName discountType:(NSString *)discountType discountAmount:(NSString *)discountAmount discountPercentage:(NSString *)discountPercentage priority:(int32_t)priority appliedTo:(NSString *)appliedTo __attribute__((swift_name("init(redeemableId:redeemableType:redeemableName:discountType:discountAmount:discountPercentage:priority:appliedTo:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicAppliedDiscount *)doCopyRedeemableId:(NSString *)redeemableId redeemableType:(NSString *)redeemableType redeemableName:(NSString *)redeemableName discountType:(NSString *)discountType discountAmount:(NSString *)discountAmount discountPercentage:(NSString *)discountPercentage priority:(int32_t)priority appliedTo:(NSString *)appliedTo __attribute__((swift_name("doCopy(redeemableId:redeemableType:redeemableName:discountType:discountAmount:discountPercentage:priority:appliedTo:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="appliedTo")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="discountAmount")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="discountPercentage")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="discountType")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="priority")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="redeemableId")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="redeemableName")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="redeemableType")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AppliedDiscount.Companion")))
@interface PromotionLogicAppliedDiscountCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicAppliedDiscountCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BudgetHold")))
@interface PromotionLogicBudgetHold : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicBudgetHoldCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString *campaignId __attribute__((swift_name("campaignId")));
@property (readonly) NSString *expiresAt __attribute__((swift_name("expiresAt")));
@property (readonly) NSString *heldAmount __attribute__((swift_name("heldAmount")));
@property (readonly) NSString *holdId __attribute__((swift_name("holdId")));
@property (readonly) NSString *redeemableId __attribute__((swift_name("redeemableId")));
- (instancetype)initWithRedeemableId:(NSString *)redeemableId holdId:(NSString *)holdId heldAmount:(NSString *)heldAmount campaignId:(NSString *)campaignId expiresAt:(NSString *)expiresAt __attribute__((swift_name("init(redeemableId:holdId:heldAmount:campaignId:expiresAt:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicBudgetHold *)doCopyRedeemableId:(NSString *)redeemableId holdId:(NSString *)holdId heldAmount:(NSString *)heldAmount campaignId:(NSString *)campaignId expiresAt:(NSString *)expiresAt __attribute__((swift_name("doCopy(redeemableId:holdId:heldAmount:campaignId:expiresAt:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="campaignId")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="expiresAt")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="heldAmount")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="holdId")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="redeemableId")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BudgetHold.Companion")))
@interface PromotionLogicBudgetHoldCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicBudgetHoldCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RedeemableRequest")))
@interface PromotionLogicRedeemableRequest : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicRedeemableRequestCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString *expectedDiscount __attribute__((swift_name("expectedDiscount")));
@property (readonly) NSDictionary<NSString *, PromotionLogicKotlinx_serialization_jsonJsonElement *> *metadata __attribute__((swift_name("metadata")));
@property (readonly) NSString *objectId __attribute__((swift_name("objectId")));
@property (readonly) NSString *objectType __attribute__((swift_name("objectType")));
@property (readonly) int32_t priority __attribute__((swift_name("priority")));
- (instancetype)initWithObjectType:(NSString *)objectType objectId:(NSString *)objectId priority:(int32_t)priority expectedDiscount:(NSString *)expectedDiscount metadata:(NSDictionary<NSString *, PromotionLogicKotlinx_serialization_jsonJsonElement *> *)metadata __attribute__((swift_name("init(objectType:objectId:priority:expectedDiscount:metadata:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicRedeemableRequest *)doCopyObjectType:(NSString *)objectType objectId:(NSString *)objectId priority:(int32_t)priority expectedDiscount:(NSString *)expectedDiscount metadata:(NSDictionary<NSString *, PromotionLogicKotlinx_serialization_jsonJsonElement *> *)metadata __attribute__((swift_name("doCopy(objectType:objectId:priority:expectedDiscount:metadata:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="expectedDiscount")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="metadata")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="objectId")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="objectType")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="priority")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RedeemableRequest.Companion")))
@interface PromotionLogicRedeemableRequestCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicRedeemableRequestCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RedemptionCustomerInfo")))
@interface PromotionLogicRedemptionCustomerInfo : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicRedemptionCustomerInfoCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString *customerType __attribute__((swift_name("customerType")));
@property (readonly) NSString *segment __attribute__((swift_name("segment")));
@property (readonly) NSString *tier __attribute__((swift_name("tier")));
- (instancetype)initWithCustomerType:(NSString *)customerType segment:(NSString *)segment tier:(NSString *)tier __attribute__((swift_name("init(customerType:segment:tier:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicRedemptionCustomerInfo *)doCopyCustomerType:(NSString *)customerType segment:(NSString *)segment tier:(NSString *)tier __attribute__((swift_name("doCopy(customerType:segment:tier:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="customerType")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="segment")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="tier")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RedemptionCustomerInfo.Companion")))
@interface PromotionLogicRedemptionCustomerInfoCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicRedemptionCustomerInfoCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RedemptionOrderInfo")))
@interface PromotionLogicRedemptionOrderInfo : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicRedemptionOrderInfoCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString *channel __attribute__((swift_name("channel")));
@property (readonly) NSString *currency __attribute__((swift_name("currency")));
@property (readonly) NSArray<PromotionLogicRedemptionOrderItem *> *items __attribute__((swift_name("items")));
@property (readonly) NSString *location __attribute__((swift_name("location")));
@property (readonly) NSString *orderDate __attribute__((swift_name("orderDate")));
@property (readonly) NSString *orderId __attribute__((swift_name("orderId")));
@property (readonly) NSString *orderValue __attribute__((swift_name("orderValue")));
- (instancetype)initWithOrderId:(NSString *)orderId orderValue:(NSString *)orderValue currency:(NSString *)currency orderDate:(NSString *)orderDate channel:(NSString *)channel location:(NSString *)location items:(NSArray<PromotionLogicRedemptionOrderItem *> *)items __attribute__((swift_name("init(orderId:orderValue:currency:orderDate:channel:location:items:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicRedemptionOrderInfo *)doCopyOrderId:(NSString *)orderId orderValue:(NSString *)orderValue currency:(NSString *)currency orderDate:(NSString *)orderDate channel:(NSString *)channel location:(NSString *)location items:(NSArray<PromotionLogicRedemptionOrderItem *> *)items __attribute__((swift_name("doCopy(orderId:orderValue:currency:orderDate:channel:location:items:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="channel")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="currency")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="items")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="location")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="orderDate")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="orderId")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="orderValue")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RedemptionOrderInfo.Companion")))
@interface PromotionLogicRedemptionOrderInfoCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicRedemptionOrderInfoCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RedemptionOrderItem")))
@interface PromotionLogicRedemptionOrderItem : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicRedemptionOrderItemCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSArray<NSString *> *collectionIds __attribute__((swift_name("collectionIds")));
@property (readonly) NSString *productId __attribute__((swift_name("productId")));
@property (readonly) int32_t quantity __attribute__((swift_name("quantity")));
@property (readonly) NSString *skuId __attribute__((swift_name("skuId")));
@property (readonly) NSString *subTotal __attribute__((swift_name("subTotal")));
@property (readonly) NSString *unitPrice __attribute__((swift_name("unitPrice")));
- (instancetype)initWithSkuId:(NSString *)skuId productId:(NSString *)productId collectionIds:(NSArray<NSString *> *)collectionIds quantity:(int32_t)quantity unitPrice:(NSString *)unitPrice subTotal:(NSString *)subTotal __attribute__((swift_name("init(skuId:productId:collectionIds:quantity:unitPrice:subTotal:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicRedemptionOrderItem *)doCopySkuId:(NSString *)skuId productId:(NSString *)productId collectionIds:(NSArray<NSString *> *)collectionIds quantity:(int32_t)quantity unitPrice:(NSString *)unitPrice subTotal:(NSString *)subTotal __attribute__((swift_name("doCopy(skuId:productId:collectionIds:quantity:unitPrice:subTotal:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="collectionIds")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="productId")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="quantity")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="skuId")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="subTotal")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="unitPrice")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RedemptionOrderItem.Companion")))
@interface PromotionLogicRedemptionOrderItemCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicRedemptionOrderItemCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RedemptionSessionRequest")))
@interface PromotionLogicRedemptionSessionRequest : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicRedemptionSessionRequestCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) PromotionLogicRedemptionCustomerInfo *customerInfo __attribute__((swift_name("customerInfo")));
@property (readonly) NSString *idempotencyKey __attribute__((swift_name("idempotencyKey")));
@property (readonly) PromotionLogicRedemptionOrderInfo *orderInfo __attribute__((swift_name("orderInfo")));
@property (readonly) NSArray<PromotionLogicRedeemableRequest *> *selectedRedeemables __attribute__((swift_name("selectedRedeemables")));
@property (readonly) PromotionLogicSessionOptions *sessionOptions __attribute__((swift_name("sessionOptions")));
- (instancetype)initWithIdempotencyKey:(NSString *)idempotencyKey customerInfo:(PromotionLogicRedemptionCustomerInfo *)customerInfo orderInfo:(PromotionLogicRedemptionOrderInfo *)orderInfo selectedRedeemables:(NSArray<PromotionLogicRedeemableRequest *> *)selectedRedeemables sessionOptions:(PromotionLogicSessionOptions *)sessionOptions __attribute__((swift_name("init(idempotencyKey:customerInfo:orderInfo:selectedRedeemables:sessionOptions:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicRedemptionSessionRequest *)doCopyIdempotencyKey:(NSString *)idempotencyKey customerInfo:(PromotionLogicRedemptionCustomerInfo *)customerInfo orderInfo:(PromotionLogicRedemptionOrderInfo *)orderInfo selectedRedeemables:(NSArray<PromotionLogicRedeemableRequest *> *)selectedRedeemables sessionOptions:(PromotionLogicSessionOptions *)sessionOptions __attribute__((swift_name("doCopy(idempotencyKey:customerInfo:orderInfo:selectedRedeemables:sessionOptions:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="customerInfo")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="idempotencyKey")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="orderInfo")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="selectedRedeemables")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="sessionOptions")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RedemptionSessionRequest.Companion")))
@interface PromotionLogicRedemptionSessionRequestCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicRedemptionSessionRequestCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RedemptionSessionResponse")))
@interface PromotionLogicRedemptionSessionResponse : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicRedemptionSessionResponseCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSArray<PromotionLogicBudgetHold *> *budgetHolds __attribute__((swift_name("budgetHolds")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *expiresAt __attribute__((swift_name("expiresAt")));
@property (readonly) PromotionLogicSessionPreview * _Nullable preview __attribute__((swift_name("preview")));
@property (readonly) NSString *sessionId __attribute__((swift_name("sessionId")));
@property (readonly) NSArray<PromotionLogicRedemptionValidationErrorResponse *> *validationErrors __attribute__((swift_name("validationErrors")));
- (instancetype)initWithSessionId:(NSString *)sessionId createdAt:(NSString *)createdAt expiresAt:(NSString *)expiresAt preview:(PromotionLogicSessionPreview * _Nullable)preview budgetHolds:(NSArray<PromotionLogicBudgetHold *> *)budgetHolds validationErrors:(NSArray<PromotionLogicRedemptionValidationErrorResponse *> *)validationErrors __attribute__((swift_name("init(sessionId:createdAt:expiresAt:preview:budgetHolds:validationErrors:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicRedemptionSessionResponse *)doCopySessionId:(NSString *)sessionId createdAt:(NSString *)createdAt expiresAt:(NSString *)expiresAt preview:(PromotionLogicSessionPreview * _Nullable)preview budgetHolds:(NSArray<PromotionLogicBudgetHold *> *)budgetHolds validationErrors:(NSArray<PromotionLogicRedemptionValidationErrorResponse *> *)validationErrors __attribute__((swift_name("doCopy(sessionId:createdAt:expiresAt:preview:budgetHolds:validationErrors:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="budgetHolds")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="createdAt")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="expiresAt")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="preview")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="sessionId")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="validationErrors")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RedemptionSessionResponse.Companion")))
@interface PromotionLogicRedemptionSessionResponseCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicRedemptionSessionResponseCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RedemptionValidationErrorResponse")))
@interface PromotionLogicRedemptionValidationErrorResponse : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicRedemptionValidationErrorResponseCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString *code __attribute__((swift_name("code")));
@property (readonly) NSString *field __attribute__((swift_name("field")));
@property (readonly) NSString *message __attribute__((swift_name("message")));
- (instancetype)initWithField:(NSString *)field code:(NSString *)code message:(NSString *)message __attribute__((swift_name("init(field:code:message:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicRedemptionValidationErrorResponse *)doCopyField:(NSString *)field code:(NSString *)code message:(NSString *)message __attribute__((swift_name("doCopy(field:code:message:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="code")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="field")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="message")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RedemptionValidationErrorResponse.Companion")))
@interface PromotionLogicRedemptionValidationErrorResponseCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicRedemptionValidationErrorResponseCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SessionOptions")))
@interface PromotionLogicSessionOptions : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicSessionOptionsCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) BOOL autoConfirm __attribute__((swift_name("autoConfirm")));
@property (readonly) BOOL holdBudget __attribute__((swift_name("holdBudget")));
@property (readonly) int32_t timeoutSeconds __attribute__((swift_name("timeoutSeconds")));
@property (readonly) BOOL validateOnly __attribute__((swift_name("validateOnly")));
- (instancetype)initWithTimeoutSeconds:(int32_t)timeoutSeconds holdBudget:(BOOL)holdBudget validateOnly:(BOOL)validateOnly autoConfirm:(BOOL)autoConfirm __attribute__((swift_name("init(timeoutSeconds:holdBudget:validateOnly:autoConfirm:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicSessionOptions *)doCopyTimeoutSeconds:(int32_t)timeoutSeconds holdBudget:(BOOL)holdBudget validateOnly:(BOOL)validateOnly autoConfirm:(BOOL)autoConfirm __attribute__((swift_name("doCopy(timeoutSeconds:holdBudget:validateOnly:autoConfirm:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="autoConfirm")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="holdBudget")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="timeoutSeconds")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="validateOnly")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SessionOptions.Companion")))
@interface PromotionLogicSessionOptionsCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicSessionOptionsCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SessionPreview")))
@interface PromotionLogicSessionPreview : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicSessionPreviewCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSArray<PromotionLogicAppliedDiscount *> *appliedDiscounts __attribute__((swift_name("appliedDiscounts")));
@property (readonly) double effectiveDiscountRate __attribute__((swift_name("effectiveDiscountRate")));
@property (readonly) NSString *finalAmount __attribute__((swift_name("finalAmount")));
@property (readonly) NSString *orderId __attribute__((swift_name("orderId")));
@property (readonly) NSString *originalAmount __attribute__((swift_name("originalAmount")));
@property (readonly) NSString *stackingMode __attribute__((swift_name("stackingMode")));
@property (readonly) NSString *totalDiscount __attribute__((swift_name("totalDiscount")));
- (instancetype)initWithOrderId:(NSString *)orderId originalAmount:(NSString *)originalAmount totalDiscount:(NSString *)totalDiscount finalAmount:(NSString *)finalAmount effectiveDiscountRate:(double)effectiveDiscountRate stackingMode:(NSString *)stackingMode appliedDiscounts:(NSArray<PromotionLogicAppliedDiscount *> *)appliedDiscounts __attribute__((swift_name("init(orderId:originalAmount:totalDiscount:finalAmount:effectiveDiscountRate:stackingMode:appliedDiscounts:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicSessionPreview *)doCopyOrderId:(NSString *)orderId originalAmount:(NSString *)originalAmount totalDiscount:(NSString *)totalDiscount finalAmount:(NSString *)finalAmount effectiveDiscountRate:(double)effectiveDiscountRate stackingMode:(NSString *)stackingMode appliedDiscounts:(NSArray<PromotionLogicAppliedDiscount *> *)appliedDiscounts __attribute__((swift_name("doCopy(orderId:originalAmount:totalDiscount:finalAmount:effectiveDiscountRate:stackingMode:appliedDiscounts:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="appliedDiscounts")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="effectiveDiscountRate")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="finalAmount")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="orderId")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="originalAmount")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="stackingMode")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="totalDiscount")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SessionPreview.Companion")))
@interface PromotionLogicSessionPreviewCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicSessionPreviewCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BusinessRuleViolation")))
@interface PromotionLogicBusinessRuleViolation : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicBusinessRuleViolationCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString * _Nullable message __attribute__((swift_name("message")));
@property (readonly) NSString * _Nullable ruleCode __attribute__((swift_name("ruleCode")));
- (instancetype)initWithRuleCode:(NSString * _Nullable)ruleCode message:(NSString * _Nullable)message __attribute__((swift_name("init(ruleCode:message:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicBusinessRuleViolation *)doCopyRuleCode:(NSString * _Nullable)ruleCode message:(NSString * _Nullable)message __attribute__((swift_name("doCopy(ruleCode:message:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="message")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="ruleCode")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BusinessRuleViolation.Companion")))
@interface PromotionLogicBusinessRuleViolationCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicBusinessRuleViolationCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DiscountDetail")))
@interface PromotionLogicDiscountDetail : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicDiscountDetailCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString *budgetStatus __attribute__((swift_name("budgetStatus")));
@property (readonly) NSString *calculatedDiscount __attribute__((swift_name("calculatedDiscount")));
@property (readonly) NSString *eligibilityStatus __attribute__((swift_name("eligibilityStatus")));
@property (readonly) NSDictionary<NSString *, PromotionLogicKotlinx_serialization_jsonJsonElement *> *metadata __attribute__((swift_name("metadata")));
@property (readonly) NSString *objectId __attribute__((swift_name("objectId")));
@property (readonly) NSString *objectType __attribute__((swift_name("objectType")));
@property (readonly) NSArray<NSString *> *tags __attribute__((swift_name("tags")));
@property (readonly) BOOL valid __attribute__((swift_name("valid")));
@property (readonly) NSArray<NSString *> *validationMessages __attribute__((swift_name("validationMessages")));
- (instancetype)initWithObjectId:(NSString *)objectId objectType:(NSString *)objectType valid:(BOOL)valid calculatedDiscount:(NSString *)calculatedDiscount eligibilityStatus:(NSString *)eligibilityStatus budgetStatus:(NSString *)budgetStatus validationMessages:(NSArray<NSString *> *)validationMessages tags:(NSArray<NSString *> *)tags metadata:(NSDictionary<NSString *, PromotionLogicKotlinx_serialization_jsonJsonElement *> *)metadata __attribute__((swift_name("init(objectId:objectType:valid:calculatedDiscount:eligibilityStatus:budgetStatus:validationMessages:tags:metadata:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicDiscountDetail *)doCopyObjectId:(NSString *)objectId objectType:(NSString *)objectType valid:(BOOL)valid calculatedDiscount:(NSString *)calculatedDiscount eligibilityStatus:(NSString *)eligibilityStatus budgetStatus:(NSString *)budgetStatus validationMessages:(NSArray<NSString *> *)validationMessages tags:(NSArray<NSString *> *)tags metadata:(NSDictionary<NSString *, PromotionLogicKotlinx_serialization_jsonJsonElement *> *)metadata __attribute__((swift_name("doCopy(objectId:objectType:valid:calculatedDiscount:eligibilityStatus:budgetStatus:validationMessages:tags:metadata:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="budgetStatus")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="calculatedDiscount")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="eligibilityStatus")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="metadata")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="objectId")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="objectType")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="tags")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="valid")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="validationMessages")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DiscountDetail.Companion")))
@interface PromotionLogicDiscountDetailCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicDiscountDetailCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DiscountRequest")))
@interface PromotionLogicDiscountRequest : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicDiscountRequestCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString *expectedDiscount __attribute__((swift_name("expectedDiscount")));
@property (readonly) NSString *maxDiscountCap __attribute__((swift_name("maxDiscountCap")));
@property (readonly) NSString *objectId __attribute__((swift_name("objectId")));
@property (readonly) NSString *objectType __attribute__((swift_name("objectType")));
@property (readonly) int32_t priority __attribute__((swift_name("priority")));
- (instancetype)initWithObjectType:(NSString *)objectType objectId:(NSString *)objectId priority:(int32_t)priority expectedDiscount:(NSString *)expectedDiscount maxDiscountCap:(NSString *)maxDiscountCap __attribute__((swift_name("init(objectType:objectId:priority:expectedDiscount:maxDiscountCap:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicDiscountRequest *)doCopyObjectType:(NSString *)objectType objectId:(NSString *)objectId priority:(int32_t)priority expectedDiscount:(NSString *)expectedDiscount maxDiscountCap:(NSString *)maxDiscountCap __attribute__((swift_name("doCopy(objectType:objectId:priority:expectedDiscount:maxDiscountCap:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="expectedDiscount")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="maxDiscountCap")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="objectId")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="objectType")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="priority")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DiscountRequest.Companion")))
@interface PromotionLogicDiscountRequestCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicDiscountRequestCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StackableCustomerInfo")))
@interface PromotionLogicStackableCustomerInfo : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicStackableCustomerInfoCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString *customerType __attribute__((swift_name("customerType")));
@property (readonly) NSString *segment __attribute__((swift_name("segment")));
@property (readonly) NSString *tier __attribute__((swift_name("tier")));
- (instancetype)initWithCustomerType:(NSString *)customerType segment:(NSString *)segment tier:(NSString *)tier __attribute__((swift_name("init(customerType:segment:tier:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicStackableCustomerInfo *)doCopyCustomerType:(NSString *)customerType segment:(NSString *)segment tier:(NSString *)tier __attribute__((swift_name("doCopy(customerType:segment:tier:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="customerType")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="segment")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="tier")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StackableCustomerInfo.Companion")))
@interface PromotionLogicStackableCustomerInfoCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicStackableCustomerInfoCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StackableDiscountsRequest")))
@interface PromotionLogicStackableDiscountsRequest : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicStackableDiscountsRequestCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) PromotionLogicStackableCustomerInfo *customerInfo __attribute__((swift_name("customerInfo")));
@property (readonly) NSArray<PromotionLogicDiscountRequest *> *discountRequests __attribute__((swift_name("discountRequests")));
@property (readonly) NSString *idempotencyKey __attribute__((swift_name("idempotencyKey")));
@property (readonly) PromotionLogicStackableOrderInfo *orderInfo __attribute__((swift_name("orderInfo")));
@property (readonly) PromotionLogicValidationOptions *validationOptions __attribute__((swift_name("validationOptions")));
- (instancetype)initWithIdempotencyKey:(NSString *)idempotencyKey customerInfo:(PromotionLogicStackableCustomerInfo *)customerInfo orderInfo:(PromotionLogicStackableOrderInfo *)orderInfo discountRequests:(NSArray<PromotionLogicDiscountRequest *> *)discountRequests validationOptions:(PromotionLogicValidationOptions *)validationOptions __attribute__((swift_name("init(idempotencyKey:customerInfo:orderInfo:discountRequests:validationOptions:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicStackableDiscountsRequest *)doCopyIdempotencyKey:(NSString *)idempotencyKey customerInfo:(PromotionLogicStackableCustomerInfo *)customerInfo orderInfo:(PromotionLogicStackableOrderInfo *)orderInfo discountRequests:(NSArray<PromotionLogicDiscountRequest *> *)discountRequests validationOptions:(PromotionLogicValidationOptions *)validationOptions __attribute__((swift_name("doCopy(idempotencyKey:customerInfo:orderInfo:discountRequests:validationOptions:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="customerInfo")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="discountRequests")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="idempotencyKey")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="orderInfo")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="validationOptions")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StackableDiscountsRequest.Companion")))
@interface PromotionLogicStackableDiscountsRequestCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicStackableDiscountsRequestCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StackableDiscountsResponse")))
@interface PromotionLogicStackableDiscountsResponse : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicStackableDiscountsResponseCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSArray<PromotionLogicBusinessRuleViolation *> *businessRuleViolations __attribute__((swift_name("businessRuleViolations")));
@property (readonly) NSString * _Nullable decisionToken __attribute__((swift_name("decisionToken")));
@property (readonly) NSArray<PromotionLogicDiscountDetail *> *discountDetails __attribute__((swift_name("discountDetails")));
@property (readonly) PromotionLogicStackingOptimization * _Nullable optimization __attribute__((swift_name("optimization")));
@property (readonly) NSString * _Nullable sessionId __attribute__((swift_name("sessionId")));
@property (readonly) PromotionLogicStackingAnalysis * _Nullable stackingAnalysis __attribute__((swift_name("stackingAnalysis")));
@property (readonly) PromotionLogicStackingValidationResult *validationResult __attribute__((swift_name("validationResult")));
@property (readonly) NSArray<NSString *> *warnings __attribute__((swift_name("warnings")));
- (instancetype)initWithValidationResult:(PromotionLogicStackingValidationResult *)validationResult decisionToken:(NSString * _Nullable)decisionToken sessionId:(NSString * _Nullable)sessionId stackingAnalysis:(PromotionLogicStackingAnalysis * _Nullable)stackingAnalysis discountDetails:(NSArray<PromotionLogicDiscountDetail *> *)discountDetails optimization:(PromotionLogicStackingOptimization * _Nullable)optimization warnings:(NSArray<NSString *> *)warnings businessRuleViolations:(NSArray<PromotionLogicBusinessRuleViolation *> *)businessRuleViolations __attribute__((swift_name("init(validationResult:decisionToken:sessionId:stackingAnalysis:discountDetails:optimization:warnings:businessRuleViolations:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicStackableDiscountsResponse *)doCopyValidationResult:(PromotionLogicStackingValidationResult *)validationResult decisionToken:(NSString * _Nullable)decisionToken sessionId:(NSString * _Nullable)sessionId stackingAnalysis:(PromotionLogicStackingAnalysis * _Nullable)stackingAnalysis discountDetails:(NSArray<PromotionLogicDiscountDetail *> *)discountDetails optimization:(PromotionLogicStackingOptimization * _Nullable)optimization warnings:(NSArray<NSString *> *)warnings businessRuleViolations:(NSArray<PromotionLogicBusinessRuleViolation *> *)businessRuleViolations __attribute__((swift_name("doCopy(validationResult:decisionToken:sessionId:stackingAnalysis:discountDetails:optimization:warnings:businessRuleViolations:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="businessRuleViolations")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="decisionToken")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="discountDetails")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="optimization")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="sessionId")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="stackingAnalysis")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="validationResult")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="warnings")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StackableDiscountsResponse.Companion")))
@interface PromotionLogicStackableDiscountsResponseCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicStackableDiscountsResponseCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StackableGroup")))
@interface PromotionLogicStackableGroup : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicStackableGroupCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString *description_ __attribute__((swift_name("description_")));
@property (readonly) NSArray<NSString *> *discounts __attribute__((swift_name("discounts")));
@property (readonly) NSString *groupDiscount __attribute__((swift_name("groupDiscount")));
@property (readonly) NSString *groupId __attribute__((swift_name("groupId")));
@property (readonly) NSString *stackingRule __attribute__((swift_name("stackingRule")));
- (instancetype)initWithGroupId:(NSString *)groupId discounts:(NSArray<NSString *> *)discounts stackingRule:(NSString *)stackingRule groupDiscount:(NSString *)groupDiscount description:(NSString *)description __attribute__((swift_name("init(groupId:discounts:stackingRule:groupDiscount:description:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicStackableGroup *)doCopyGroupId:(NSString *)groupId discounts:(NSArray<NSString *> *)discounts stackingRule:(NSString *)stackingRule groupDiscount:(NSString *)groupDiscount description:(NSString *)description __attribute__((swift_name("doCopy(groupId:discounts:stackingRule:groupDiscount:description:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="description")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="discounts")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="groupDiscount")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="groupId")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="stackingRule")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StackableGroup.Companion")))
@interface PromotionLogicStackableGroupCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicStackableGroupCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StackableOrderInfo")))
@interface PromotionLogicStackableOrderInfo : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicStackableOrderInfoCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString *channel __attribute__((swift_name("channel")));
@property (readonly) NSString *currency __attribute__((swift_name("currency")));
@property (readonly) NSArray<PromotionLogicStackableOrderItem *> *items __attribute__((swift_name("items")));
@property (readonly) NSString *location __attribute__((swift_name("location")));
@property (readonly) NSString *orderDate __attribute__((swift_name("orderDate")));
@property (readonly) NSString *orderId __attribute__((swift_name("orderId")));
@property (readonly) NSString *orderValue __attribute__((swift_name("orderValue")));
- (instancetype)initWithOrderId:(NSString *)orderId orderValue:(NSString *)orderValue currency:(NSString *)currency orderDate:(NSString *)orderDate channel:(NSString *)channel location:(NSString *)location items:(NSArray<PromotionLogicStackableOrderItem *> *)items __attribute__((swift_name("init(orderId:orderValue:currency:orderDate:channel:location:items:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicStackableOrderInfo *)doCopyOrderId:(NSString *)orderId orderValue:(NSString *)orderValue currency:(NSString *)currency orderDate:(NSString *)orderDate channel:(NSString *)channel location:(NSString *)location items:(NSArray<PromotionLogicStackableOrderItem *> *)items __attribute__((swift_name("doCopy(orderId:orderValue:currency:orderDate:channel:location:items:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="channel")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="currency")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="items")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="location")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="orderDate")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="orderId")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="orderValue")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StackableOrderInfo.Companion")))
@interface PromotionLogicStackableOrderInfoCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicStackableOrderInfoCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StackableOrderItem")))
@interface PromotionLogicStackableOrderItem : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicStackableOrderItemCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString *category __attribute__((swift_name("category")));
@property (readonly) NSArray<NSString *> *collectionIds __attribute__((swift_name("collectionIds")));
@property (readonly) NSString *price __attribute__((swift_name("price")));
@property (readonly) NSString *productId __attribute__((swift_name("productId")));
@property (readonly) int32_t quantity __attribute__((swift_name("quantity")));
@property (readonly) NSString *sku __attribute__((swift_name("sku")));
- (instancetype)initWithSku:(NSString *)sku productId:(NSString *)productId collectionIds:(NSArray<NSString *> *)collectionIds quantity:(int32_t)quantity price:(NSString *)price category:(NSString *)category __attribute__((swift_name("init(sku:productId:collectionIds:quantity:price:category:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicStackableOrderItem *)doCopySku:(NSString *)sku productId:(NSString *)productId collectionIds:(NSArray<NSString *> *)collectionIds quantity:(int32_t)quantity price:(NSString *)price category:(NSString *)category __attribute__((swift_name("doCopy(sku:productId:collectionIds:quantity:price:category:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="category")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="collectionIds")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="price")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="productId")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="quantity")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="sku")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StackableOrderItem.Companion")))
@interface PromotionLogicStackableOrderItemCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicStackableOrderItemCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StackingAnalysis")))
@interface PromotionLogicStackingAnalysis : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicStackingAnalysisCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSArray<PromotionLogicStackingConflict *> *conflicts __attribute__((swift_name("conflicts")));
@property (readonly) NSArray<PromotionLogicStackingExclusion *> *exclusions __attribute__((swift_name("exclusions")));
@property (readonly) NSArray<PromotionLogicStackableGroup *> *stackableGroups __attribute__((swift_name("stackableGroups")));
- (instancetype)initWithStackableGroups:(NSArray<PromotionLogicStackableGroup *> *)stackableGroups conflicts:(NSArray<PromotionLogicStackingConflict *> *)conflicts exclusions:(NSArray<PromotionLogicStackingExclusion *> *)exclusions __attribute__((swift_name("init(stackableGroups:conflicts:exclusions:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicStackingAnalysis *)doCopyStackableGroups:(NSArray<PromotionLogicStackableGroup *> *)stackableGroups conflicts:(NSArray<PromotionLogicStackingConflict *> *)conflicts exclusions:(NSArray<PromotionLogicStackingExclusion *> *)exclusions __attribute__((swift_name("doCopy(stackableGroups:conflicts:exclusions:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="conflicts")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="exclusions")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="stackableGroups")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StackingAnalysis.Companion")))
@interface PromotionLogicStackingAnalysisCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicStackingAnalysisCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StackingConflict")))
@interface PromotionLogicStackingConflict : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicStackingConflictCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString *conflictType __attribute__((swift_name("conflictType")));
@property (readonly) NSArray<NSString *> *discounts __attribute__((swift_name("discounts")));
@property (readonly) NSString *reason __attribute__((swift_name("reason")));
- (instancetype)initWithConflictType:(NSString *)conflictType discounts:(NSArray<NSString *> *)discounts reason:(NSString *)reason __attribute__((swift_name("init(conflictType:discounts:reason:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicStackingConflict *)doCopyConflictType:(NSString *)conflictType discounts:(NSArray<NSString *> *)discounts reason:(NSString *)reason __attribute__((swift_name("doCopy(conflictType:discounts:reason:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="conflictType")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="discounts")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="reason")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StackingConflict.Companion")))
@interface PromotionLogicStackingConflictCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicStackingConflictCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StackingExclusion")))
@interface PromotionLogicStackingExclusion : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicStackingExclusionCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString *excludedObjectId __attribute__((swift_name("excludedObjectId")));
@property (readonly) NSString *reason __attribute__((swift_name("reason")));
- (instancetype)initWithExcludedObjectId:(NSString *)excludedObjectId reason:(NSString *)reason __attribute__((swift_name("init(excludedObjectId:reason:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicStackingExclusion *)doCopyExcludedObjectId:(NSString *)excludedObjectId reason:(NSString *)reason __attribute__((swift_name("doCopy(excludedObjectId:reason:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="excludedObjectId")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="reason")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StackingExclusion.Companion")))
@interface PromotionLogicStackingExclusionCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicStackingExclusionCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StackingOptimization")))
@interface PromotionLogicStackingOptimization : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicStackingOptimizationCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSArray<PromotionLogicKotlinx_serialization_jsonJsonElement *> *alternativeStacks __attribute__((swift_name("alternativeStacks")));
@property (readonly) NSString *maxPossibleDiscount __attribute__((swift_name("maxPossibleDiscount")));
@property (readonly) NSArray<NSString *> *optimizationNotes __attribute__((swift_name("optimizationNotes")));
@property (readonly) NSArray<NSString *> *recommendedOrder __attribute__((swift_name("recommendedOrder")));
- (instancetype)initWithRecommendedOrder:(NSArray<NSString *> *)recommendedOrder alternativeStacks:(NSArray<PromotionLogicKotlinx_serialization_jsonJsonElement *> *)alternativeStacks maxPossibleDiscount:(NSString *)maxPossibleDiscount optimizationNotes:(NSArray<NSString *> *)optimizationNotes __attribute__((swift_name("init(recommendedOrder:alternativeStacks:maxPossibleDiscount:optimizationNotes:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicStackingOptimization *)doCopyRecommendedOrder:(NSArray<NSString *> *)recommendedOrder alternativeStacks:(NSArray<PromotionLogicKotlinx_serialization_jsonJsonElement *> *)alternativeStacks maxPossibleDiscount:(NSString *)maxPossibleDiscount optimizationNotes:(NSArray<NSString *> *)optimizationNotes __attribute__((swift_name("doCopy(recommendedOrder:alternativeStacks:maxPossibleDiscount:optimizationNotes:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="alternativeStacks")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="maxPossibleDiscount")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="optimizationNotes")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="recommendedOrder")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StackingOptimization.Companion")))
@interface PromotionLogicStackingOptimizationCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicStackingOptimizationCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StackingValidationResult")))
@interface PromotionLogicStackingValidationResult : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicStackingValidationResultCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) BOOL canStack __attribute__((swift_name("canStack")));
@property (readonly) double effectiveDiscountRate __attribute__((swift_name("effectiveDiscountRate")));
@property (readonly) NSString *finalAmount __attribute__((swift_name("finalAmount")));
@property (readonly) BOOL overallValid __attribute__((swift_name("overallValid")));
@property (readonly) NSString *totalDiscountAmount __attribute__((swift_name("totalDiscountAmount")));
@property (readonly) NSString *validationSummary __attribute__((swift_name("validationSummary")));
- (instancetype)initWithOverallValid:(BOOL)overallValid canStack:(BOOL)canStack totalDiscountAmount:(NSString *)totalDiscountAmount finalAmount:(NSString *)finalAmount effectiveDiscountRate:(double)effectiveDiscountRate validationSummary:(NSString *)validationSummary __attribute__((swift_name("init(overallValid:canStack:totalDiscountAmount:finalAmount:effectiveDiscountRate:validationSummary:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicStackingValidationResult *)doCopyOverallValid:(BOOL)overallValid canStack:(BOOL)canStack totalDiscountAmount:(NSString *)totalDiscountAmount finalAmount:(NSString *)finalAmount effectiveDiscountRate:(double)effectiveDiscountRate validationSummary:(NSString *)validationSummary __attribute__((swift_name("doCopy(overallValid:canStack:totalDiscountAmount:finalAmount:effectiveDiscountRate:validationSummary:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="canStack")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="effectiveDiscountRate")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="finalAmount")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="overallValid")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="totalDiscountAmount")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="validationSummary")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StackingValidationResult.Companion")))
@interface PromotionLogicStackingValidationResultCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicStackingValidationResultCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ValidationOptions")))
@interface PromotionLogicValidationOptions : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicValidationOptionsCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) BOOL checkBudgetAvailability __attribute__((swift_name("checkBudgetAvailability")));
@property (readonly) BOOL dryRun __attribute__((swift_name("dryRun")));
@property (readonly) NSString *explainLevel __attribute__((swift_name("explainLevel")));
@property (readonly) BOOL includeAlternatives __attribute__((swift_name("includeAlternatives")));
@property (readonly) BOOL optimizeOrder __attribute__((swift_name("optimizeOrder")));
- (instancetype)initWithCheckBudgetAvailability:(BOOL)checkBudgetAvailability optimizeOrder:(BOOL)optimizeOrder explainLevel:(NSString *)explainLevel includeAlternatives:(BOOL)includeAlternatives dryRun:(BOOL)dryRun __attribute__((swift_name("init(checkBudgetAvailability:optimizeOrder:explainLevel:includeAlternatives:dryRun:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicValidationOptions *)doCopyCheckBudgetAvailability:(BOOL)checkBudgetAvailability optimizeOrder:(BOOL)optimizeOrder explainLevel:(NSString *)explainLevel includeAlternatives:(BOOL)includeAlternatives dryRun:(BOOL)dryRun __attribute__((swift_name("doCopy(checkBudgetAvailability:optimizeOrder:explainLevel:includeAlternatives:dryRun:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="checkBudgetAvailability")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="dryRun")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="explainLevel")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="includeAlternatives")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="optimizeOrder")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ValidationOptions.Companion")))
@interface PromotionLogicValidationOptionsCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicValidationOptionsCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ApplicableProductDto")))
@interface PromotionLogicApplicableProductDto : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicApplicableProductDtoCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString * _Nullable image __attribute__((swift_name("image")));
@property (readonly) NSString * _Nullable itemType __attribute__((swift_name("itemType")));
@property (readonly) NSString * _Nullable name __attribute__((swift_name("name")));
@property (readonly) NSString * _Nullable productId __attribute__((swift_name("productId")));
@property (readonly) NSString * _Nullable productSourceId __attribute__((swift_name("productSourceId")));
@property (readonly) NSString * _Nullable sku __attribute__((swift_name("sku")));
@property (readonly) NSString * _Nullable skuSourceId __attribute__((swift_name("skuSourceId")));
@property (readonly) NSString * _Nullable type __attribute__((swift_name("type")));
- (instancetype)initWithProductId:(NSString * _Nullable)productId skuSourceId:(NSString * _Nullable)skuSourceId productSourceId:(NSString * _Nullable)productSourceId sku:(NSString * _Nullable)sku name:(NSString * _Nullable)name image:(NSString * _Nullable)image type:(NSString * _Nullable)type itemType:(NSString * _Nullable)itemType __attribute__((swift_name("init(productId:skuSourceId:productSourceId:sku:name:image:type:itemType:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicApplicableProductDto *)doCopyProductId:(NSString * _Nullable)productId skuSourceId:(NSString * _Nullable)skuSourceId productSourceId:(NSString * _Nullable)productSourceId sku:(NSString * _Nullable)sku name:(NSString * _Nullable)name image:(NSString * _Nullable)image type:(NSString * _Nullable)type itemType:(NSString * _Nullable)itemType __attribute__((swift_name("doCopy(productId:skuSourceId:productSourceId:sku:name:image:type:itemType:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="image")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="itemType")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="name")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="productId")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="productSourceId")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="sku")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="skuSourceId")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="type")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ApplicableProductDto.Companion")))
@interface PromotionLogicApplicableProductDtoCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicApplicableProductDtoCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CustomerVoucherDetail")))
@interface PromotionLogicCustomerVoucherDetail : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicCustomerVoucherDetailCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) PromotionLogicDouble * _Nullable amount __attribute__((swift_name("amount")));
@property (readonly) NSArray<PromotionLogicApplicableProductDto *> *applicableProducts __attribute__((swift_name("applicableProducts")));
@property (readonly) NSArray<PromotionLogicVoucherCodeDto *> *codes __attribute__((swift_name("codes")));
@property (readonly) NSString * _Nullable endDate __attribute__((swift_name("endDate")));
@property (readonly) PromotionLogicDouble * _Nullable expireWarningDate __attribute__((swift_name("expireWarningDate")));
@property (readonly) PromotionLogicInt * _Nullable expiredTimeNumber __attribute__((swift_name("expiredTimeNumber")));
@property (readonly) PromotionLogicInt * _Nullable isYourself __attribute__((swift_name("isYourself")));
@property (readonly) PromotionLogicVoucherMetadataDto * _Nullable metadata __attribute__((swift_name("metadata")));
@property (readonly) PromotionLogicInt * _Nullable priority __attribute__((swift_name("priority")));
@property (readonly) PromotionLogicInt * _Nullable quantity __attribute__((swift_name("quantity")));
@property (readonly) NSString * _Nullable startDate __attribute__((swift_name("startDate")));
@property (readonly) PromotionLogicDouble * _Nullable value __attribute__((swift_name("value")));
@property (readonly) PromotionLogicVoucherInfoDto *voucher __attribute__((swift_name("voucher")));
- (instancetype)initWithVoucher:(PromotionLogicVoucherInfoDto *)voucher codes:(NSArray<PromotionLogicVoucherCodeDto *> *)codes quantity:(PromotionLogicInt * _Nullable)quantity value:(PromotionLogicDouble * _Nullable)value amount:(PromotionLogicDouble * _Nullable)amount startDate:(NSString * _Nullable)startDate endDate:(NSString * _Nullable)endDate expiredTimeNumber:(PromotionLogicInt * _Nullable)expiredTimeNumber priority:(PromotionLogicInt * _Nullable)priority isYourself:(PromotionLogicInt * _Nullable)isYourself metadata:(PromotionLogicVoucherMetadataDto * _Nullable)metadata applicableProducts:(NSArray<PromotionLogicApplicableProductDto *> *)applicableProducts expireWarningDate:(PromotionLogicDouble * _Nullable)expireWarningDate __attribute__((swift_name("init(voucher:codes:quantity:value:amount:startDate:endDate:expiredTimeNumber:priority:isYourself:metadata:applicableProducts:expireWarningDate:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicCustomerVoucherDetail *)doCopyVoucher:(PromotionLogicVoucherInfoDto *)voucher codes:(NSArray<PromotionLogicVoucherCodeDto *> *)codes quantity:(PromotionLogicInt * _Nullable)quantity value:(PromotionLogicDouble * _Nullable)value amount:(PromotionLogicDouble * _Nullable)amount startDate:(NSString * _Nullable)startDate endDate:(NSString * _Nullable)endDate expiredTimeNumber:(PromotionLogicInt * _Nullable)expiredTimeNumber priority:(PromotionLogicInt * _Nullable)priority isYourself:(PromotionLogicInt * _Nullable)isYourself metadata:(PromotionLogicVoucherMetadataDto * _Nullable)metadata applicableProducts:(NSArray<PromotionLogicApplicableProductDto *> *)applicableProducts expireWarningDate:(PromotionLogicDouble * _Nullable)expireWarningDate __attribute__((swift_name("doCopy(voucher:codes:quantity:value:amount:startDate:endDate:expiredTimeNumber:priority:isYourself:metadata:applicableProducts:expireWarningDate:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="amount")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="applicableProducts")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="codes")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="endDate")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="expireWarningDate")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="expiredTimeNumber")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="isYourself")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="metadata")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="priority")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="quantity")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="startDate")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="value")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="voucher")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CustomerVoucherDetail.Companion")))
@interface PromotionLogicCustomerVoucherDetailCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicCustomerVoucherDetailCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PageableInfo")))
@interface PromotionLogicPageableInfo : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicPageableInfoCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) PromotionLogicLong * _Nullable offset __attribute__((swift_name("offset")));
@property (readonly) PromotionLogicInt * _Nullable pageNumber __attribute__((swift_name("pageNumber")));
@property (readonly) PromotionLogicInt * _Nullable pageSize __attribute__((swift_name("pageSize")));
@property (readonly) PromotionLogicBoolean * _Nullable paged __attribute__((swift_name("paged")));
@property (readonly) PromotionLogicSortInfo * _Nullable sort __attribute__((swift_name("sort")));
@property (readonly) PromotionLogicBoolean * _Nullable unpaged __attribute__((swift_name("unpaged")));
- (instancetype)initWithPageNumber:(PromotionLogicInt * _Nullable)pageNumber pageSize:(PromotionLogicInt * _Nullable)pageSize offset:(PromotionLogicLong * _Nullable)offset paged:(PromotionLogicBoolean * _Nullable)paged unpaged:(PromotionLogicBoolean * _Nullable)unpaged sort:(PromotionLogicSortInfo * _Nullable)sort __attribute__((swift_name("init(pageNumber:pageSize:offset:paged:unpaged:sort:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicPageableInfo *)doCopyPageNumber:(PromotionLogicInt * _Nullable)pageNumber pageSize:(PromotionLogicInt * _Nullable)pageSize offset:(PromotionLogicLong * _Nullable)offset paged:(PromotionLogicBoolean * _Nullable)paged unpaged:(PromotionLogicBoolean * _Nullable)unpaged sort:(PromotionLogicSortInfo * _Nullable)sort __attribute__((swift_name("doCopy(pageNumber:pageSize:offset:paged:unpaged:sort:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="offset")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="pageNumber")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="pageSize")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="paged")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="sort")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="unpaged")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PageableInfo.Companion")))
@interface PromotionLogicPageableInfoCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicPageableInfoCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SearchCustomerVouchersResponse")))
@interface PromotionLogicSearchCustomerVouchersResponse : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicSearchCustomerVouchersResponseCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSArray<PromotionLogicVoucherListItem *> *content __attribute__((swift_name("content")));
@property (readonly) NSString * _Nullable defaultTab __attribute__((swift_name("defaultTab")));
@property (readonly) PromotionLogicBoolean * _Nullable empty __attribute__((swift_name("empty")));
@property (readonly) PromotionLogicDouble * _Nullable expireWarningDate __attribute__((swift_name("expireWarningDate")));
@property (readonly) PromotionLogicBoolean * _Nullable first __attribute__((swift_name("first")));
@property (readonly) NSString * _Nullable keyword __attribute__((swift_name("keyword")));
@property (readonly) PromotionLogicBoolean * _Nullable last __attribute__((swift_name("last")));
@property (readonly) PromotionLogicInt * _Nullable number __attribute__((swift_name("number")));
@property (readonly) PromotionLogicInt * _Nullable numberOfElements __attribute__((swift_name("numberOfElements")));
@property (readonly) PromotionLogicPageableInfo * _Nullable pageable __attribute__((swift_name("pageable")));
@property (readonly) NSString * _Nullable selectedTab __attribute__((swift_name("selectedTab")));
@property (readonly) NSString * _Nullable serviceCode __attribute__((swift_name("serviceCode")));
@property (readonly) PromotionLogicInt * _Nullable size __attribute__((swift_name("size")));
@property (readonly) PromotionLogicSortInfo * _Nullable sort __attribute__((swift_name("sort")));
@property (readonly) NSArray<PromotionLogicVoucherTabInfo *> *tabs __attribute__((swift_name("tabs")));
@property (readonly) PromotionLogicLong * _Nullable totalElements __attribute__((swift_name("totalElements")));
@property (readonly) PromotionLogicInt * _Nullable totalPages __attribute__((swift_name("totalPages")));
- (instancetype)initWithKeyword:(NSString * _Nullable)keyword serviceCode:(NSString * _Nullable)serviceCode expireWarningDate:(PromotionLogicDouble * _Nullable)expireWarningDate tabs:(NSArray<PromotionLogicVoucherTabInfo *> *)tabs defaultTab:(NSString * _Nullable)defaultTab selectedTab:(NSString * _Nullable)selectedTab content:(NSArray<PromotionLogicVoucherListItem *> *)content pageable:(PromotionLogicPageableInfo * _Nullable)pageable totalElements:(PromotionLogicLong * _Nullable)totalElements totalPages:(PromotionLogicInt * _Nullable)totalPages first:(PromotionLogicBoolean * _Nullable)first last:(PromotionLogicBoolean * _Nullable)last number:(PromotionLogicInt * _Nullable)number size:(PromotionLogicInt * _Nullable)size numberOfElements:(PromotionLogicInt * _Nullable)numberOfElements empty:(PromotionLogicBoolean * _Nullable)empty sort:(PromotionLogicSortInfo * _Nullable)sort __attribute__((swift_name("init(keyword:serviceCode:expireWarningDate:tabs:defaultTab:selectedTab:content:pageable:totalElements:totalPages:first:last:number:size:numberOfElements:empty:sort:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicSearchCustomerVouchersResponse *)doCopyKeyword:(NSString * _Nullable)keyword serviceCode:(NSString * _Nullable)serviceCode expireWarningDate:(PromotionLogicDouble * _Nullable)expireWarningDate tabs:(NSArray<PromotionLogicVoucherTabInfo *> *)tabs defaultTab:(NSString * _Nullable)defaultTab selectedTab:(NSString * _Nullable)selectedTab content:(NSArray<PromotionLogicVoucherListItem *> *)content pageable:(PromotionLogicPageableInfo * _Nullable)pageable totalElements:(PromotionLogicLong * _Nullable)totalElements totalPages:(PromotionLogicInt * _Nullable)totalPages first:(PromotionLogicBoolean * _Nullable)first last:(PromotionLogicBoolean * _Nullable)last number:(PromotionLogicInt * _Nullable)number size:(PromotionLogicInt * _Nullable)size numberOfElements:(PromotionLogicInt * _Nullable)numberOfElements empty:(PromotionLogicBoolean * _Nullable)empty sort:(PromotionLogicSortInfo * _Nullable)sort __attribute__((swift_name("doCopy(keyword:serviceCode:expireWarningDate:tabs:defaultTab:selectedTab:content:pageable:totalElements:totalPages:first:last:number:size:numberOfElements:empty:sort:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="content")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="defaultTab")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="empty")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="expireWarningDate")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="first")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="keyword")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="last")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="number")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="numberOfElements")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="pageable")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="selectedTab")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="serviceCode")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="size")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="sort")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="tabs")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="totalElements")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="totalPages")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SearchCustomerVouchersResponse.Companion")))
@interface PromotionLogicSearchCustomerVouchersResponseCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicSearchCustomerVouchersResponseCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SortInfo")))
@interface PromotionLogicSortInfo : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicSortInfoCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) PromotionLogicBoolean * _Nullable empty __attribute__((swift_name("empty")));
@property (readonly) PromotionLogicBoolean * _Nullable sorted __attribute__((swift_name("sorted")));
@property (readonly) PromotionLogicBoolean * _Nullable unsorted __attribute__((swift_name("unsorted")));
- (instancetype)initWithSorted:(PromotionLogicBoolean * _Nullable)sorted unsorted:(PromotionLogicBoolean * _Nullable)unsorted empty:(PromotionLogicBoolean * _Nullable)empty __attribute__((swift_name("init(sorted:unsorted:empty:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicSortInfo *)doCopySorted:(PromotionLogicBoolean * _Nullable)sorted unsorted:(PromotionLogicBoolean * _Nullable)unsorted empty:(PromotionLogicBoolean * _Nullable)empty __attribute__((swift_name("doCopy(sorted:unsorted:empty:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="empty")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="sorted")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="unsorted")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SortInfo.Companion")))
@interface PromotionLogicSortInfoCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicSortInfoCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VoucherBrandDto")))
@interface PromotionLogicVoucherBrandDto : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicVoucherBrandDtoCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSArray<NSString *> *logo __attribute__((swift_name("logo")));
@property (readonly) NSString * _Nullable name __attribute__((swift_name("name")));
- (instancetype)initWithName:(NSString * _Nullable)name logo:(NSArray<NSString *> *)logo __attribute__((swift_name("init(name:logo:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicVoucherBrandDto *)doCopyName:(NSString * _Nullable)name logo:(NSArray<NSString *> *)logo __attribute__((swift_name("doCopy(name:logo:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="logo")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="name")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VoucherBrandDto.Companion")))
@interface PromotionLogicVoucherBrandDtoCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicVoucherBrandDtoCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VoucherCodeDto")))
@interface PromotionLogicVoucherCodeDto : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicVoucherCodeDtoCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString * _Nullable codex __attribute__((swift_name("codex")));
@property (readonly) NSString * _Nullable expiredAt __attribute__((swift_name("expiredAt")));
@property (readonly) NSString * _Nullable phone __attribute__((swift_name("phone")));
@property (readonly) NSString * _Nullable pickedUpAt __attribute__((swift_name("pickedUpAt")));
- (instancetype)initWithPhone:(NSString * _Nullable)phone codex:(NSString * _Nullable)codex expiredAt:(NSString * _Nullable)expiredAt pickedUpAt:(NSString * _Nullable)pickedUpAt __attribute__((swift_name("init(phone:codex:expiredAt:pickedUpAt:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicVoucherCodeDto *)doCopyPhone:(NSString * _Nullable)phone codex:(NSString * _Nullable)codex expiredAt:(NSString * _Nullable)expiredAt pickedUpAt:(NSString * _Nullable)pickedUpAt __attribute__((swift_name("doCopy(phone:codex:expiredAt:pickedUpAt:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="codex")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="expiredAt")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="phone")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="pickedUpAt")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VoucherCodeDto.Companion")))
@interface PromotionLogicVoucherCodeDtoCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicVoucherCodeDtoCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VoucherInfoDto")))
@interface PromotionLogicVoucherInfoDto : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicVoucherInfoDtoCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSArray<PromotionLogicApplicableProductDto *> *applicableProducts __attribute__((swift_name("applicableProducts")));
@property (readonly) PromotionLogicVoucherBrandDto * _Nullable brand __attribute__((swift_name("brand")));
@property (readonly) NSString * _Nullable campaignEndDate __attribute__((swift_name("campaignEndDate")));
@property (readonly) NSString * _Nullable content __attribute__((swift_name("content")));
@property (readonly) NSString * _Nullable description_ __attribute__((swift_name("description_")));
@property (readonly) PromotionLogicInt * _Nullable discountType __attribute__((swift_name("discountType")));
@property (readonly) PromotionLogicDouble * _Nullable discountValue __attribute__((swift_name("discountValue")));
@property (readonly) NSString * _Nullable displayStatusLabel __attribute__((swift_name("displayStatusLabel")));
@property (readonly) NSString * _Nullable endDate __attribute__((swift_name("endDate")));
@property (readonly) NSString * _Nullable expiredTime __attribute__((swift_name("expiredTime")));
@property (readonly) NSString * _Nullable guideline __attribute__((swift_name("guideline")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString * _Nullable image __attribute__((swift_name("image")));
@property (readonly) PromotionLogicDouble * _Nullable maxDiscount __attribute__((swift_name("maxDiscount")));
@property (readonly) PromotionLogicDouble * _Nullable minOrder __attribute__((swift_name("minOrder")));
@property (readonly) PromotionLogicInt * _Nullable remainingQty __attribute__((swift_name("remainingQty")));
@property (readonly) NSString * _Nullable status __attribute__((swift_name("status")));
@property (readonly) NSArray<NSString *> *tags __attribute__((swift_name("tags")));
@property (readonly) NSString * _Nullable title __attribute__((swift_name("title")));
@property (readonly) PromotionLogicBoolean * _Nullable unlimitedQty __attribute__((swift_name("unlimitedQty")));
@property (readonly) PromotionLogicInt * _Nullable usedQty __attribute__((swift_name("usedQty")));
- (instancetype)initWithId:(NSString *)id brand:(PromotionLogicVoucherBrandDto * _Nullable)brand image:(NSString * _Nullable)image title:(NSString * _Nullable)title remainingQty:(PromotionLogicInt * _Nullable)remainingQty usedQty:(PromotionLogicInt * _Nullable)usedQty discountType:(PromotionLogicInt * _Nullable)discountType discountValue:(PromotionLogicDouble * _Nullable)discountValue maxDiscount:(PromotionLogicDouble * _Nullable)maxDiscount minOrder:(PromotionLogicDouble * _Nullable)minOrder content:(NSString * _Nullable)content description:(NSString * _Nullable)description guideline:(NSString * _Nullable)guideline tags:(NSArray<NSString *> *)tags endDate:(NSString * _Nullable)endDate campaignEndDate:(NSString * _Nullable)campaignEndDate expiredTime:(NSString * _Nullable)expiredTime unlimitedQty:(PromotionLogicBoolean * _Nullable)unlimitedQty status:(NSString * _Nullable)status displayStatusLabel:(NSString * _Nullable)displayStatusLabel applicableProducts:(NSArray<PromotionLogicApplicableProductDto *> *)applicableProducts __attribute__((swift_name("init(id:brand:image:title:remainingQty:usedQty:discountType:discountValue:maxDiscount:minOrder:content:description:guideline:tags:endDate:campaignEndDate:expiredTime:unlimitedQty:status:displayStatusLabel:applicableProducts:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicVoucherInfoDto *)doCopyId:(NSString *)id brand:(PromotionLogicVoucherBrandDto * _Nullable)brand image:(NSString * _Nullable)image title:(NSString * _Nullable)title remainingQty:(PromotionLogicInt * _Nullable)remainingQty usedQty:(PromotionLogicInt * _Nullable)usedQty discountType:(PromotionLogicInt * _Nullable)discountType discountValue:(PromotionLogicDouble * _Nullable)discountValue maxDiscount:(PromotionLogicDouble * _Nullable)maxDiscount minOrder:(PromotionLogicDouble * _Nullable)minOrder content:(NSString * _Nullable)content description:(NSString * _Nullable)description guideline:(NSString * _Nullable)guideline tags:(NSArray<NSString *> *)tags endDate:(NSString * _Nullable)endDate campaignEndDate:(NSString * _Nullable)campaignEndDate expiredTime:(NSString * _Nullable)expiredTime unlimitedQty:(PromotionLogicBoolean * _Nullable)unlimitedQty status:(NSString * _Nullable)status displayStatusLabel:(NSString * _Nullable)displayStatusLabel applicableProducts:(NSArray<PromotionLogicApplicableProductDto *> *)applicableProducts __attribute__((swift_name("doCopy(id:brand:image:title:remainingQty:usedQty:discountType:discountValue:maxDiscount:minOrder:content:description:guideline:tags:endDate:campaignEndDate:expiredTime:unlimitedQty:status:displayStatusLabel:applicableProducts:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="applicableProducts")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="brand")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="campaignEndDate")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="content")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="description")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="discountType")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="discountValue")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="displayStatusLabel")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="endDate")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="expiredTime")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="guideline")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="id")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="image")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="maxDiscount")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="minOrder")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="remainingQty")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="status")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="tags")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="title")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="unlimitedQty")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="usedQty")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VoucherInfoDto.Companion")))
@interface PromotionLogicVoucherInfoDtoCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicVoucherInfoDtoCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VoucherListItem")))
@interface PromotionLogicVoucherListItem : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicVoucherListItemCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) PromotionLogicDouble * _Nullable amount __attribute__((swift_name("amount")));
@property (readonly) NSArray<PromotionLogicApplicableProductDto *> *applicableProducts __attribute__((swift_name("applicableProducts")));
@property (readonly) NSString * _Nullable endDate __attribute__((swift_name("endDate")));
@property (readonly) PromotionLogicInt * _Nullable expiredTimeNumber __attribute__((swift_name("expiredTimeNumber")));
@property (readonly) PromotionLogicInt * _Nullable isYourself __attribute__((swift_name("isYourself")));
@property (readonly) PromotionLogicVoucherMetadataDto * _Nullable metadata __attribute__((swift_name("metadata")));
@property (readonly) PromotionLogicInt * _Nullable priority __attribute__((swift_name("priority")));
@property (readonly) PromotionLogicInt * _Nullable quantity __attribute__((swift_name("quantity")));
@property (readonly) NSString * _Nullable startDate __attribute__((swift_name("startDate")));
@property (readonly) PromotionLogicDouble * _Nullable value __attribute__((swift_name("value")));
@property (readonly) PromotionLogicVoucherInfoDto *voucher __attribute__((swift_name("voucher")));
- (instancetype)initWithVoucher:(PromotionLogicVoucherInfoDto *)voucher quantity:(PromotionLogicInt * _Nullable)quantity value:(PromotionLogicDouble * _Nullable)value amount:(PromotionLogicDouble * _Nullable)amount startDate:(NSString * _Nullable)startDate endDate:(NSString * _Nullable)endDate expiredTimeNumber:(PromotionLogicInt * _Nullable)expiredTimeNumber priority:(PromotionLogicInt * _Nullable)priority isYourself:(PromotionLogicInt * _Nullable)isYourself metadata:(PromotionLogicVoucherMetadataDto * _Nullable)metadata applicableProducts:(NSArray<PromotionLogicApplicableProductDto *> *)applicableProducts __attribute__((swift_name("init(voucher:quantity:value:amount:startDate:endDate:expiredTimeNumber:priority:isYourself:metadata:applicableProducts:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicVoucherListItem *)doCopyVoucher:(PromotionLogicVoucherInfoDto *)voucher quantity:(PromotionLogicInt * _Nullable)quantity value:(PromotionLogicDouble * _Nullable)value amount:(PromotionLogicDouble * _Nullable)amount startDate:(NSString * _Nullable)startDate endDate:(NSString * _Nullable)endDate expiredTimeNumber:(PromotionLogicInt * _Nullable)expiredTimeNumber priority:(PromotionLogicInt * _Nullable)priority isYourself:(PromotionLogicInt * _Nullable)isYourself metadata:(PromotionLogicVoucherMetadataDto * _Nullable)metadata applicableProducts:(NSArray<PromotionLogicApplicableProductDto *> *)applicableProducts __attribute__((swift_name("doCopy(voucher:quantity:value:amount:startDate:endDate:expiredTimeNumber:priority:isYourself:metadata:applicableProducts:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="amount")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="applicableProducts")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="endDate")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="expiredTimeNumber")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="isYourself")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="metadata")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="priority")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="quantity")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="startDate")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="value")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="voucher")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VoucherListItem.Companion")))
@interface PromotionLogicVoucherListItemCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicVoucherListItemCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VoucherMetadataDto")))
@interface PromotionLogicVoucherMetadataDto : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicVoucherMetadataDtoCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString * _Nullable disabledReason __attribute__((swift_name("disabledReason")));
@property (readonly) NSString * _Nullable displayMode __attribute__((swift_name("displayMode")));
@property (readonly) NSString * _Nullable eligibilityScore __attribute__((swift_name("eligibilityScore")));
@property (readonly) NSString * _Nullable matchedRules __attribute__((swift_name("matchedRules")));
@property (readonly) NSString * _Nullable usable __attribute__((swift_name("usable")));
@property (readonly) NSString * _Nullable usageGuideUrl __attribute__((swift_name("usageGuideUrl")));
- (instancetype)initWithUsable:(NSString * _Nullable)usable displayMode:(NSString * _Nullable)displayMode disabledReason:(NSString * _Nullable)disabledReason usageGuideUrl:(NSString * _Nullable)usageGuideUrl eligibilityScore:(NSString * _Nullable)eligibilityScore matchedRules:(NSString * _Nullable)matchedRules __attribute__((swift_name("init(usable:displayMode:disabledReason:usageGuideUrl:eligibilityScore:matchedRules:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicVoucherMetadataDto *)doCopyUsable:(NSString * _Nullable)usable displayMode:(NSString * _Nullable)displayMode disabledReason:(NSString * _Nullable)disabledReason usageGuideUrl:(NSString * _Nullable)usageGuideUrl eligibilityScore:(NSString * _Nullable)eligibilityScore matchedRules:(NSString * _Nullable)matchedRules __attribute__((swift_name("doCopy(usable:displayMode:disabledReason:usageGuideUrl:eligibilityScore:matchedRules:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="disabledReason")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="displayMode")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="eligibilityScore")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="matchedRules")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="usable")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="usageGuideUrl")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VoucherMetadataDto.Companion")))
@interface PromotionLogicVoucherMetadataDtoCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicVoucherMetadataDtoCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VoucherTabInfo")))
@interface PromotionLogicVoucherTabInfo : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicVoucherTabInfoCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString *code __attribute__((swift_name("code")));
@property (readonly) PromotionLogicInt * _Nullable count __attribute__((swift_name("count")));
@property (readonly, getter=default) PromotionLogicBoolean * _Nullable default_ __attribute__((swift_name("default_")));
@property (readonly) NSString *label __attribute__((swift_name("label")));
@property (readonly) NSDictionary<NSString *, NSString *> * _Nullable labelI18n __attribute__((swift_name("labelI18n")));
@property (readonly) PromotionLogicInt * _Nullable order __attribute__((swift_name("order")));
- (instancetype)initWithCode:(NSString *)code label:(NSString *)label labelI18n:(NSDictionary<NSString *, NSString *> * _Nullable)labelI18n default:(PromotionLogicBoolean * _Nullable)default_ count:(PromotionLogicInt * _Nullable)count order:(PromotionLogicInt * _Nullable)order __attribute__((swift_name("init(code:label:labelI18n:default:count:order:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicVoucherTabInfo *)doCopyCode:(NSString *)code label:(NSString *)label labelI18n:(NSDictionary<NSString *, NSString *> * _Nullable)labelI18n default:(PromotionLogicBoolean * _Nullable)default_ count:(PromotionLogicInt * _Nullable)count order:(PromotionLogicInt * _Nullable)order __attribute__((swift_name("doCopy(code:label:labelI18n:default:count:order:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="code")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="count")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="default")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="label")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="labelI18n")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="order")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VoucherTabInfo.Companion")))
@interface PromotionLogicVoucherTabInfoCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicVoucherTabInfoCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
__attribute__((swift_name("PromotionPreferences")))
@protocol PromotionLogicPromotionPreferences
@required
- (void)clear __attribute__((swift_name("clear()")));
- (BOOL)containsKey:(NSString *)key __attribute__((swift_name("contains(key:)")));
- (BOOL)getBooleanKey:(NSString *)key default:(BOOL)default_ __attribute__((swift_name("getBoolean(key:default:)")));
- (NSString * _Nullable)getStringKey:(NSString *)key __attribute__((swift_name("getString(key:)")));
- (void)putBooleanKey:(NSString *)key value:(BOOL)value __attribute__((swift_name("putBoolean(key:value:)")));
- (void)putStringKey:(NSString *)key value:(NSString *)value __attribute__((swift_name("putString(key:value:)")));
- (void)removeKey:(NSString *)key __attribute__((swift_name("remove(key:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PromotionPreferencesCompanion")))
@interface PromotionLogicPromotionPreferencesCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicPromotionPreferencesCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *PREFS_NAME __attribute__((swift_name("PREFS_NAME")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ApiResponseTemplate")))
@interface PromotionLogicApiResponseTemplate<T> : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicApiResponseTemplateCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString * _Nullable code __attribute__((swift_name("code")));
@property (readonly) T _Nullable data __attribute__((swift_name("data")));
@property (readonly) NSString * _Nullable message __attribute__((swift_name("message")));
@property (readonly) PromotionLogicResponseMetadata * _Nullable metadata __attribute__((swift_name("metadata")));
@property (readonly) PromotionLogicInt * _Nullable status __attribute__((swift_name("status")));
@property (readonly) PromotionLogicBoolean * _Nullable success __attribute__((swift_name("success")));
@property (readonly) NSString * _Nullable timestamp __attribute__((swift_name("timestamp")));
- (instancetype)initWithStatus:(PromotionLogicInt * _Nullable)status code:(NSString * _Nullable)code success:(PromotionLogicBoolean * _Nullable)success message:(NSString * _Nullable)message timestamp:(NSString * _Nullable)timestamp metadata:(PromotionLogicResponseMetadata * _Nullable)metadata data:(T _Nullable)data __attribute__((swift_name("init(status:code:success:message:timestamp:metadata:data:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicApiResponseTemplate<T> *)doCopyStatus:(PromotionLogicInt * _Nullable)status code:(NSString * _Nullable)code success:(PromotionLogicBoolean * _Nullable)success message:(NSString * _Nullable)message timestamp:(NSString * _Nullable)timestamp metadata:(PromotionLogicResponseMetadata * _Nullable)metadata data:(T _Nullable)data __attribute__((swift_name("doCopy(status:code:success:message:timestamp:metadata:data:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="code")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="data")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="message")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="metadata")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="status")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="success")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="timestamp")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ApiResponseTemplateCompanion")))
@interface PromotionLogicApiResponseTemplateCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicApiResponseTemplateCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializerTypeParamsSerializers:(PromotionLogicKotlinArray<id<PromotionLogicKotlinx_serialization_coreKSerializer>> *)typeParamsSerializers __attribute__((swift_name("serializer(typeParamsSerializers:)")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializerTypeSerial0:(id<PromotionLogicKotlinx_serialization_coreKSerializer>)typeSerial0 __attribute__((swift_name("serializer(typeSerial0:)")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ResponseMetadata")))
@interface PromotionLogicResponseMetadata : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicResponseMetadataCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) PromotionLogicBoolean * _Nullable partial __attribute__((swift_name("partial")));
@property (readonly) NSString * _Nullable requestId __attribute__((swift_name("requestId")));
- (instancetype)initWithRequestId:(NSString * _Nullable)requestId partial:(PromotionLogicBoolean * _Nullable)partial __attribute__((swift_name("init(requestId:partial:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicResponseMetadata *)doCopyRequestId:(NSString * _Nullable)requestId partial:(PromotionLogicBoolean * _Nullable)partial __attribute__((swift_name("doCopy(requestId:partial:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="partial")
*/
/**
 * @note annotations
 *   kotlinx.serialization.SerialName(value="requestId")
*/
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ResponseMetadata.Companion")))
@interface PromotionLogicResponseMetadataCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicResponseMetadataCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PromotionContainer")))
@interface PromotionLogicPromotionContainer : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicPromotionContainer *shared __attribute__((swift_name("shared")));
@property (readonly) id<PromotionLogicPromotionPreferences> preferences __attribute__((swift_name("preferences")));
@property (readonly) id<PromotionLogicPromotionRequestContextProvider> requestContextProvider __attribute__((swift_name("requestContextProvider")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)promotionContainer __attribute__((swift_name("init()")));
- (void)clear __attribute__((swift_name("clear()")));
- (void)initializeConfig:(PromotionLogicPromotionSDKConfig *)config __attribute__((swift_name("initialize(config:)")));
- (BOOL)isInitialized __attribute__((swift_name("isInitialized()")));
- (PromotionLogicPromotionSDKConfig *)requireConfig __attribute__((swift_name("requireConfig()")));
@end
__attribute__((swift_name("KotlinThrowable")))
@interface PromotionLogicKotlinThrowable : PromotionLogicBase
@property (readonly) PromotionLogicKotlinThrowable * _Nullable cause __attribute__((swift_name("cause")));
@property (readonly) NSString * _Nullable message __attribute__((swift_name("message")));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(PromotionLogicKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(PromotionLogicKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
/**
 * @note annotations
 *   kotlin.experimental.ExperimentalNativeApi
*/
- (PromotionLogicKotlinArray<NSString *> *)getStackTrace __attribute__((swift_name("getStackTrace()")));
- (void)printStackTrace __attribute__((swift_name("printStackTrace()")));
- (NSString *)description __attribute__((swift_name("description()")));
- (NSError *)asError __attribute__((swift_name("asError()")));
@end
__attribute__((swift_name("KotlinException")))
@interface PromotionLogicKotlinException : PromotionLogicKotlinThrowable
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(PromotionLogicKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(PromotionLogicKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FeatureFlagException")))
@interface PromotionLogicFeatureFlagException : PromotionLogicKotlinException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithCause:(PromotionLogicKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(PromotionLogicKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end
__attribute__((swift_name("KotlinRuntimeException")))
@interface PromotionLogicKotlinRuntimeException : PromotionLogicKotlinException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(PromotionLogicKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(PromotionLogicKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NetworkException")))
@interface PromotionLogicNetworkException : PromotionLogicKotlinRuntimeException
@property (readonly) NSString *errorCode __attribute__((swift_name("errorCode")));
@property (readonly) NSString * _Nullable message __attribute__((swift_name("message")));
- (instancetype)initWithErrorCode:(NSString *)errorCode message:(NSString * _Nullable)message cause:(PromotionLogicKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(errorCode:message:cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithCause:(PromotionLogicKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(PromotionLogicKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PromotionErrorCodes")))
@interface PromotionLogicPromotionErrorCodes : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicPromotionErrorCodes *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *FEATURE_DISABLED __attribute__((swift_name("FEATURE_DISABLED")));
@property (readonly) NSString *GENERAL __attribute__((swift_name("GENERAL")));
@property (readonly) NSString *INSUFFICIENT_BUDGET __attribute__((swift_name("INSUFFICIENT_BUDGET")));
@property (readonly) NSString *MISSING_CUSTOMER_ID __attribute__((swift_name("MISSING_CUSTOMER_ID")));
@property (readonly) NSString *NETWORK_ERROR __attribute__((swift_name("NETWORK_ERROR")));
@property (readonly) NSString *NO_RESULT __attribute__((swift_name("NO_RESULT")));
@property (readonly) NSString *TIMEOUT __attribute__((swift_name("TIMEOUT")));
@property (readonly) NSString *TOKEN_EXPIRED __attribute__((swift_name("TOKEN_EXPIRED")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)promotionErrorCodes __attribute__((swift_name("init()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PromotionException")))
@interface PromotionLogicPromotionException : PromotionLogicKotlinRuntimeException
@property (readonly) NSString * _Nullable errorCode __attribute__((swift_name("errorCode")));
@property (readonly) PromotionLogicInt * _Nullable httpStatus __attribute__((swift_name("httpStatus")));
@property (readonly) NSString * _Nullable message __attribute__((swift_name("message")));
- (instancetype)initWithErrorCode:(NSString * _Nullable)errorCode message:(NSString * _Nullable)message httpStatus:(PromotionLogicInt * _Nullable)httpStatus __attribute__((swift_name("init(errorCode:message:httpStatus:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithCause:(PromotionLogicKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(PromotionLogicKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@end
__attribute__((swift_name("PromotionResult")))
@protocol PromotionLogicPromotionResult
@required
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PromotionResultFailure")))
@interface PromotionLogicPromotionResultFailure : PromotionLogicBase <PromotionLogicPromotionResult>
@property (readonly) NSString *errorCode __attribute__((swift_name("errorCode")));
@property (readonly) PromotionLogicInt * _Nullable httpStatus __attribute__((swift_name("httpStatus")));
@property (readonly) NSString * _Nullable message __attribute__((swift_name("message")));
- (instancetype)initWithErrorCode:(NSString *)errorCode message:(NSString * _Nullable)message httpStatus:(PromotionLogicInt * _Nullable)httpStatus __attribute__((swift_name("init(errorCode:message:httpStatus:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicPromotionResultFailure *)doCopyErrorCode:(NSString *)errorCode message:(NSString * _Nullable)message httpStatus:(PromotionLogicInt * _Nullable)httpStatus __attribute__((swift_name("doCopy(errorCode:message:httpStatus:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PromotionResultSuccess")))
@interface PromotionLogicPromotionResultSuccess<__covariant T> : PromotionLogicBase <PromotionLogicPromotionResult>
@property (readonly) T _Nullable data __attribute__((swift_name("data")));
- (instancetype)initWithData:(T _Nullable)data __attribute__((swift_name("init(data:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicPromotionResultSuccess<T> *)doCopyData:(T _Nullable)data __attribute__((swift_name("doCopy(data:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EligibleFilterOptions")))
@interface PromotionLogicEligibleFilterOptions : PromotionLogicBase
@property (readonly) NSArray<NSString *> * _Nullable campaignTypes __attribute__((swift_name("campaignTypes")));
@property (readonly) BOOL checkBudgetAvailability __attribute__((swift_name("checkBudgetAvailability")));
@property (readonly) NSArray<NSString *> * _Nullable discountTypes __attribute__((swift_name("discountTypes")));
@property (readonly) BOOL includeExpired __attribute__((swift_name("includeExpired")));
@property (readonly) BOOL includePreview __attribute__((swift_name("includePreview")));
- (instancetype)initWithCampaignTypes:(NSArray<NSString *> * _Nullable)campaignTypes discountTypes:(NSArray<NSString *> * _Nullable)discountTypes includeExpired:(BOOL)includeExpired checkBudgetAvailability:(BOOL)checkBudgetAvailability includePreview:(BOOL)includePreview __attribute__((swift_name("init(campaignTypes:discountTypes:includeExpired:checkBudgetAvailability:includePreview:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicEligibleFilterOptions *)doCopyCampaignTypes:(NSArray<NSString *> * _Nullable)campaignTypes discountTypes:(NSArray<NSString *> * _Nullable)discountTypes includeExpired:(BOOL)includeExpired checkBudgetAvailability:(BOOL)checkBudgetAvailability includePreview:(BOOL)includePreview __attribute__((swift_name("doCopy(campaignTypes:discountTypes:includeExpired:checkBudgetAvailability:includePreview:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EligibleOffer")))
@interface PromotionLogicEligibleOffer : PromotionLogicBase
@property (readonly) PromotionLogicBoolean * _Nullable budgetAvailable __attribute__((swift_name("budgetAvailable")));
@property (readonly) NSString * _Nullable campaignId __attribute__((swift_name("campaignId")));
@property (readonly) NSString * _Nullable campaignName __attribute__((swift_name("campaignName")));
@property (readonly) NSString * _Nullable campaignType __attribute__((swift_name("campaignType")));
@property (readonly) NSString * _Nullable discountPercentage __attribute__((swift_name("discountPercentage")));
@property (readonly) NSString * _Nullable discountType __attribute__((swift_name("discountType")));
@property (readonly) NSString * _Nullable displayName __attribute__((swift_name("displayName")));
@property (readonly) NSString * _Nullable estimatedDiscount __attribute__((swift_name("estimatedDiscount")));
@property (readonly) NSString * _Nullable expireDate __attribute__((swift_name("expireDate")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) BOOL isOwnedVoucher __attribute__((swift_name("isOwnedVoucher")));
@property (readonly) NSString * _Nullable logoUrl __attribute__((swift_name("logoUrl")));
@property (readonly) NSString * _Nullable maxDiscount __attribute__((swift_name("maxDiscount")));
@property (readonly) NSString * _Nullable minOrderValue __attribute__((swift_name("minOrderValue")));
@property (readonly) NSString *objectType __attribute__((swift_name("objectType")));
@property (readonly) NSString * _Nullable partnerName __attribute__((swift_name("partnerName")));
@property (readonly) PromotionLogicInt * _Nullable remainingRedemptions __attribute__((swift_name("remainingRedemptions")));
@property (readonly) NSString * _Nullable startDate __attribute__((swift_name("startDate")));
@property (readonly) NSArray<NSString *> *unmatchedRules __attribute__((swift_name("unmatchedRules")));
@property (readonly) BOOL usable __attribute__((swift_name("usable")));
@property (readonly) NSString * _Nullable voucherCode __attribute__((swift_name("voucherCode")));
@property (readonly) NSString * _Nullable voucherId __attribute__((swift_name("voucherId")));
@property (readonly) NSString * _Nullable voucherName __attribute__((swift_name("voucherName")));
- (instancetype)initWithId:(NSString *)id campaignId:(NSString * _Nullable)campaignId voucherId:(NSString * _Nullable)voucherId campaignName:(NSString * _Nullable)campaignName voucherName:(NSString * _Nullable)voucherName campaignType:(NSString * _Nullable)campaignType objectType:(NSString *)objectType discountType:(NSString * _Nullable)discountType usable:(BOOL)usable logoUrl:(NSString * _Nullable)logoUrl partnerName:(NSString * _Nullable)partnerName voucherCode:(NSString * _Nullable)voucherCode estimatedDiscount:(NSString * _Nullable)estimatedDiscount discountPercentage:(NSString * _Nullable)discountPercentage maxDiscount:(NSString * _Nullable)maxDiscount minOrderValue:(NSString * _Nullable)minOrderValue startDate:(NSString * _Nullable)startDate expireDate:(NSString * _Nullable)expireDate remainingRedemptions:(PromotionLogicInt * _Nullable)remainingRedemptions budgetAvailable:(PromotionLogicBoolean * _Nullable)budgetAvailable unmatchedRules:(NSArray<NSString *> *)unmatchedRules __attribute__((swift_name("init(id:campaignId:voucherId:campaignName:voucherName:campaignType:objectType:discountType:usable:logoUrl:partnerName:voucherCode:estimatedDiscount:discountPercentage:maxDiscount:minOrderValue:startDate:expireDate:remainingRedemptions:budgetAvailable:unmatchedRules:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicEligibleOffer *)doCopyId:(NSString *)id campaignId:(NSString * _Nullable)campaignId voucherId:(NSString * _Nullable)voucherId campaignName:(NSString * _Nullable)campaignName voucherName:(NSString * _Nullable)voucherName campaignType:(NSString * _Nullable)campaignType objectType:(NSString *)objectType discountType:(NSString * _Nullable)discountType usable:(BOOL)usable logoUrl:(NSString * _Nullable)logoUrl partnerName:(NSString * _Nullable)partnerName voucherCode:(NSString * _Nullable)voucherCode estimatedDiscount:(NSString * _Nullable)estimatedDiscount discountPercentage:(NSString * _Nullable)discountPercentage maxDiscount:(NSString * _Nullable)maxDiscount minOrderValue:(NSString * _Nullable)minOrderValue startDate:(NSString * _Nullable)startDate expireDate:(NSString * _Nullable)expireDate remainingRedemptions:(PromotionLogicInt * _Nullable)remainingRedemptions budgetAvailable:(PromotionLogicBoolean * _Nullable)budgetAvailable unmatchedRules:(NSArray<NSString *> *)unmatchedRules __attribute__((swift_name("doCopy(id:campaignId:voucherId:campaignName:voucherName:campaignType:objectType:discountType:usable:logoUrl:partnerName:voucherCode:estimatedDiscount:discountPercentage:maxDiscount:minOrderValue:startDate:expireDate:remainingRedemptions:budgetAvailable:unmatchedRules:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EligibleOffersResult")))
@interface PromotionLogicEligibleOffersResult : PromotionLogicBase
@property (readonly) NSString * _Nullable activeTab __attribute__((swift_name("activeTab")));
@property (readonly) PromotionLogicInt * _Nullable expireWarningDate __attribute__((swift_name("expireWarningDate")));
@property (readonly) BOOL myIsLastPage __attribute__((swift_name("myIsLastPage")));
@property (readonly) NSArray<PromotionLogicEligibleOffer *> *myOffers __attribute__((swift_name("myOffers")));
@property (readonly) int64_t myTotalElements __attribute__((swift_name("myTotalElements")));
@property (readonly) BOOL otherIsLastPage __attribute__((swift_name("otherIsLastPage")));
@property (readonly) NSArray<PromotionLogicEligibleOffer *> *otherOffers __attribute__((swift_name("otherOffers")));
@property (readonly) int64_t otherTotalElements __attribute__((swift_name("otherTotalElements")));
@property (readonly) NSArray<PromotionLogicVoucherTabItem *> *tabs __attribute__((swift_name("tabs")));
- (instancetype)initWithMyOffers:(NSArray<PromotionLogicEligibleOffer *> *)myOffers otherOffers:(NSArray<PromotionLogicEligibleOffer *> *)otherOffers tabs:(NSArray<PromotionLogicVoucherTabItem *> *)tabs activeTab:(NSString * _Nullable)activeTab myIsLastPage:(BOOL)myIsLastPage otherIsLastPage:(BOOL)otherIsLastPage myTotalElements:(int64_t)myTotalElements otherTotalElements:(int64_t)otherTotalElements expireWarningDate:(PromotionLogicInt * _Nullable)expireWarningDate __attribute__((swift_name("init(myOffers:otherOffers:tabs:activeTab:myIsLastPage:otherIsLastPage:myTotalElements:otherTotalElements:expireWarningDate:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicEligibleOffersResult *)doCopyMyOffers:(NSArray<PromotionLogicEligibleOffer *> *)myOffers otherOffers:(NSArray<PromotionLogicEligibleOffer *> *)otherOffers tabs:(NSArray<PromotionLogicVoucherTabItem *> *)tabs activeTab:(NSString * _Nullable)activeTab myIsLastPage:(BOOL)myIsLastPage otherIsLastPage:(BOOL)otherIsLastPage myTotalElements:(int64_t)myTotalElements otherTotalElements:(int64_t)otherTotalElements expireWarningDate:(PromotionLogicInt * _Nullable)expireWarningDate __attribute__((swift_name("doCopy(myOffers:otherOffers:tabs:activeTab:myIsLastPage:otherIsLastPage:myTotalElements:otherTotalElements:expireWarningDate:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EligibleOrderItem")))
@interface PromotionLogicEligibleOrderItem : PromotionLogicBase
@property (readonly) NSString * _Nullable orderItemId __attribute__((swift_name("orderItemId")));
@property (readonly) NSString * _Nullable productCategory __attribute__((swift_name("productCategory")));
@property (readonly) NSString * _Nullable productId __attribute__((swift_name("productId")));
@property (readonly) NSString * _Nullable productName __attribute__((swift_name("productName")));
@property (readonly) int32_t quantity __attribute__((swift_name("quantity")));
@property (readonly) NSString *skuSourceId __attribute__((swift_name("skuSourceId")));
@property (readonly) NSString *unitPrice __attribute__((swift_name("unitPrice")));
- (instancetype)initWithSkuSourceId:(NSString *)skuSourceId quantity:(int32_t)quantity unitPrice:(NSString *)unitPrice orderItemId:(NSString * _Nullable)orderItemId productId:(NSString * _Nullable)productId productName:(NSString * _Nullable)productName productCategory:(NSString * _Nullable)productCategory __attribute__((swift_name("init(skuSourceId:quantity:unitPrice:orderItemId:productId:productName:productCategory:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicEligibleOrderItem *)doCopySkuSourceId:(NSString *)skuSourceId quantity:(int32_t)quantity unitPrice:(NSString *)unitPrice orderItemId:(NSString * _Nullable)orderItemId productId:(NSString * _Nullable)productId productName:(NSString * _Nullable)productName productCategory:(NSString * _Nullable)productCategory __attribute__((swift_name("doCopy(skuSourceId:quantity:unitPrice:orderItemId:productId:productName:productCategory:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EligibleSection")))
@interface PromotionLogicEligibleSection : PromotionLogicKotlinEnum<PromotionLogicEligibleSection *>
@property (class, readonly) PromotionLogicEligibleSection *myOffers __attribute__((swift_name("myOffers")));
@property (class, readonly) PromotionLogicEligibleSection *otherOffers __attribute__((swift_name("otherOffers")));
@property (class, readonly) NSArray<PromotionLogicEligibleSection *> *entries __attribute__((swift_name("entries")));
@property (readonly) NSString *code __attribute__((swift_name("code")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (PromotionLogicKotlinArray<PromotionLogicEligibleSection *> *)values __attribute__((swift_name("values()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FindEligibleCampaignsRequest")))
@interface PromotionLogicFindEligibleCampaignsRequest : PromotionLogicBase
@property (readonly) NSString *channel __attribute__((swift_name("channel")));
@property (readonly) NSString *currency __attribute__((swift_name("currency")));
@property (readonly) NSString * _Nullable customerType __attribute__((swift_name("customerType")));
@property (readonly) PromotionLogicEligibleFilterOptions *filterOptions __attribute__((swift_name("filterOptions")));
@property (readonly) NSArray<PromotionLogicEligibleOrderItem *> *items __attribute__((swift_name("items")));
@property (readonly) NSString * _Nullable keyword __attribute__((swift_name("keyword")));
@property (readonly) int32_t myPage __attribute__((swift_name("myPage")));
@property (readonly) int32_t mySize __attribute__((swift_name("mySize")));
@property (readonly) NSString * _Nullable orderDate __attribute__((swift_name("orderDate")));
@property (readonly) NSString *orderId __attribute__((swift_name("orderId")));
@property (readonly) NSDictionary<NSString *, NSString *> * _Nullable orderMetadata __attribute__((swift_name("orderMetadata")));
@property (readonly) NSString *orderValue __attribute__((swift_name("orderValue")));
@property (readonly) int32_t otherPage __attribute__((swift_name("otherPage")));
@property (readonly) int32_t otherSize __attribute__((swift_name("otherSize")));
@property (readonly) NSString * _Nullable scenario __attribute__((swift_name("scenario")));
@property (readonly) PromotionLogicEligibleSection * _Nullable section __attribute__((swift_name("section")));
@property (readonly) NSString * _Nullable segment __attribute__((swift_name("segment")));
@property (readonly) NSString * _Nullable tabCode __attribute__((swift_name("tabCode")));
@property (readonly) NSString * _Nullable tier __attribute__((swift_name("tier")));
- (instancetype)initWithOrderId:(NSString *)orderId orderValue:(NSString *)orderValue items:(NSArray<PromotionLogicEligibleOrderItem *> *)items currency:(NSString *)currency channel:(NSString *)channel customerType:(NSString * _Nullable)customerType segment:(NSString * _Nullable)segment tier:(NSString * _Nullable)tier orderDate:(NSString * _Nullable)orderDate orderMetadata:(NSDictionary<NSString *, NSString *> * _Nullable)orderMetadata scenario:(NSString * _Nullable)scenario tabCode:(NSString * _Nullable)tabCode section:(PromotionLogicEligibleSection * _Nullable)section keyword:(NSString * _Nullable)keyword myPage:(int32_t)myPage mySize:(int32_t)mySize otherPage:(int32_t)otherPage otherSize:(int32_t)otherSize filterOptions:(PromotionLogicEligibleFilterOptions *)filterOptions __attribute__((swift_name("init(orderId:orderValue:items:currency:channel:customerType:segment:tier:orderDate:orderMetadata:scenario:tabCode:section:keyword:myPage:mySize:otherPage:otherSize:filterOptions:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicFindEligibleCampaignsRequest *)doCopyOrderId:(NSString *)orderId orderValue:(NSString *)orderValue items:(NSArray<PromotionLogicEligibleOrderItem *> *)items currency:(NSString *)currency channel:(NSString *)channel customerType:(NSString * _Nullable)customerType segment:(NSString * _Nullable)segment tier:(NSString * _Nullable)tier orderDate:(NSString * _Nullable)orderDate orderMetadata:(NSDictionary<NSString *, NSString *> * _Nullable)orderMetadata scenario:(NSString * _Nullable)scenario tabCode:(NSString * _Nullable)tabCode section:(PromotionLogicEligibleSection * _Nullable)section keyword:(NSString * _Nullable)keyword myPage:(int32_t)myPage mySize:(int32_t)mySize otherPage:(int32_t)otherPage otherSize:(int32_t)otherSize filterOptions:(PromotionLogicEligibleFilterOptions *)filterOptions __attribute__((swift_name("doCopy(orderId:orderValue:items:currency:channel:customerType:segment:tier:orderDate:orderMetadata:scenario:tabCode:section:keyword:myPage:mySize:otherPage:otherSize:filterOptions:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (PromotionLogicFindEligibleCampaignsRequest *)forSectionPageSection:(PromotionLogicEligibleSection * _Nullable)section nextPage:(int32_t)nextPage currentMyPage:(int32_t)currentMyPage currentOtherPage:(int32_t)currentOtherPage __attribute__((swift_name("forSectionPage(section:nextPage:currentMyPage:currentOtherPage:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FeatureFlag")))
@interface PromotionLogicFeatureFlag : PromotionLogicBase
@property (readonly) BOOL enabled __attribute__((swift_name("enabled")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
- (instancetype)initWithName:(NSString *)name enabled:(BOOL)enabled __attribute__((swift_name("init(name:enabled:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicFeatureFlag *)doCopyName:(NSString *)name enabled:(BOOL)enabled __attribute__((swift_name("doCopy(name:enabled:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PromotionFeatureFlag")))
@interface PromotionLogicPromotionFeatureFlag : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicPromotionFeatureFlag *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *ENABLE_ALL __attribute__((swift_name("ENABLE_ALL")));
@property (readonly) NSString *VOUCHER_APPLY __attribute__((swift_name("VOUCHER_APPLY")));
@property (readonly) NSString *VOUCHER_DETAIL __attribute__((swift_name("VOUCHER_DETAIL")));
@property (readonly) NSString *VOUCHER_LIST __attribute__((swift_name("VOUCHER_LIST")));
@property (readonly) NSString *VOUCHER_REDEEM __attribute__((swift_name("VOUCHER_REDEEM")));
@property (readonly) NSString *VOUCHER_SELECTION __attribute__((swift_name("VOUCHER_SELECTION")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)promotionFeatureFlag __attribute__((swift_name("init()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PromotionFeatureFlags")))
@interface PromotionLogicPromotionFeatureFlags : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicPromotionFeatureFlagsCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) BOOL enableAll __attribute__((swift_name("enableAll")));
@property (readonly) BOOL voucherApply __attribute__((swift_name("voucherApply")));
@property (readonly) BOOL voucherDetail __attribute__((swift_name("voucherDetail")));
@property (readonly) BOOL voucherList __attribute__((swift_name("voucherList")));
@property (readonly) BOOL voucherRedeem __attribute__((swift_name("voucherRedeem")));
@property (readonly) BOOL voucherSelection __attribute__((swift_name("voucherSelection")));
- (instancetype)initWithEnableAll:(BOOL)enableAll voucherApply:(BOOL)voucherApply voucherRedeem:(BOOL)voucherRedeem voucherSelection:(BOOL)voucherSelection voucherDetail:(BOOL)voucherDetail voucherList:(BOOL)voucherList __attribute__((swift_name("init(enableAll:voucherApply:voucherRedeem:voucherSelection:voucherDetail:voucherList:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicPromotionFeatureFlags *)doCopyEnableAll:(BOOL)enableAll voucherApply:(BOOL)voucherApply voucherRedeem:(BOOL)voucherRedeem voucherSelection:(BOOL)voucherSelection voucherDetail:(BOOL)voucherDetail voucherList:(BOOL)voucherList __attribute__((swift_name("doCopy(enableAll:voucherApply:voucherRedeem:voucherSelection:voucherDetail:voucherList:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (BOOL)isEnabledFlag:(NSString *)flag __attribute__((swift_name("isEnabled(flag:)")));
- (PromotionLogicPromotionFeatureFlags *)normalized __attribute__((swift_name("normalized()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PromotionFeatureFlags.Companion")))
@interface PromotionLogicPromotionFeatureFlagsCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicPromotionFeatureFlagsCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) PromotionLogicPromotionFeatureFlags *AllDisabled __attribute__((swift_name("AllDisabled")));
@property (readonly) PromotionLogicPromotionFeatureFlags *AllEnabled __attribute__((swift_name("AllEnabled")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CreateRedemptionRequest")))
@interface PromotionLogicCreateRedemptionRequest : PromotionLogicBase
@property (readonly) NSArray<PromotionLogicRedemptionItemRequest *> *items __attribute__((swift_name("items")));
@property (readonly) NSString *orderId __attribute__((swift_name("orderId")));
@property (readonly) NSString *orderValue __attribute__((swift_name("orderValue")));
- (instancetype)initWithOrderId:(NSString *)orderId orderValue:(NSString *)orderValue items:(NSArray<PromotionLogicRedemptionItemRequest *> *)items __attribute__((swift_name("init(orderId:orderValue:items:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicCreateRedemptionRequest *)doCopyOrderId:(NSString *)orderId orderValue:(NSString *)orderValue items:(NSArray<PromotionLogicRedemptionItemRequest *> *)items __attribute__((swift_name("doCopy(orderId:orderValue:items:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CreateRedemptionResult")))
@interface PromotionLogicCreateRedemptionResult : PromotionLogicBase
@property (readonly) NSString *finalAmount __attribute__((swift_name("finalAmount")));
@property (readonly) BOOL hasBudgetError __attribute__((swift_name("hasBudgetError")));
@property (readonly) BOOL hasErrors __attribute__((swift_name("hasErrors")));
@property (readonly) NSString *sessionId __attribute__((swift_name("sessionId")));
@property (readonly) NSString *totalDiscount __attribute__((swift_name("totalDiscount")));
@property (readonly) NSArray<PromotionLogicRedemptionValidationError *> *validationErrors __attribute__((swift_name("validationErrors")));
- (instancetype)initWithSessionId:(NSString *)sessionId totalDiscount:(NSString *)totalDiscount finalAmount:(NSString *)finalAmount validationErrors:(NSArray<PromotionLogicRedemptionValidationError *> *)validationErrors __attribute__((swift_name("init(sessionId:totalDiscount:finalAmount:validationErrors:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicCreateRedemptionResult *)doCopySessionId:(NSString *)sessionId totalDiscount:(NSString *)totalDiscount finalAmount:(NSString *)finalAmount validationErrors:(NSArray<PromotionLogicRedemptionValidationError *> *)validationErrors __attribute__((swift_name("doCopy(sessionId:totalDiscount:finalAmount:validationErrors:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RedemptionItemRequest")))
@interface PromotionLogicRedemptionItemRequest : PromotionLogicBase
@property (readonly) NSString * _Nullable expectedDiscount __attribute__((swift_name("expectedDiscount")));
@property (readonly) NSString *objectId __attribute__((swift_name("objectId")));
@property (readonly) NSString *objectType __attribute__((swift_name("objectType")));
- (instancetype)initWithObjectId:(NSString *)objectId objectType:(NSString *)objectType expectedDiscount:(NSString * _Nullable)expectedDiscount __attribute__((swift_name("init(objectId:objectType:expectedDiscount:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicRedemptionItemRequest *)doCopyObjectId:(NSString *)objectId objectType:(NSString *)objectType expectedDiscount:(NSString * _Nullable)expectedDiscount __attribute__((swift_name("doCopy(objectId:objectType:expectedDiscount:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RedemptionValidationError")))
@interface PromotionLogicRedemptionValidationError : PromotionLogicBase
@property (readonly) NSString *code __attribute__((swift_name("code")));
@property (readonly) NSString *message __attribute__((swift_name("message")));
- (instancetype)initWithCode:(NSString *)code message:(NSString *)message __attribute__((swift_name("init(code:message:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicRedemptionValidationError *)doCopyCode:(NSString *)code message:(NSString *)message __attribute__((swift_name("doCopy(code:message:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DiscountItemRequest")))
@interface PromotionLogicDiscountItemRequest : PromotionLogicBase
@property (readonly) NSString *objectId __attribute__((swift_name("objectId")));
@property (readonly) NSString *objectType __attribute__((swift_name("objectType")));
- (instancetype)initWithObjectId:(NSString *)objectId objectType:(NSString *)objectType __attribute__((swift_name("init(objectId:objectType:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicDiscountItemRequest *)doCopyObjectId:(NSString *)objectId objectType:(NSString *)objectType __attribute__((swift_name("doCopy(objectId:objectType:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DiscountItemResult")))
@interface PromotionLogicDiscountItemResult : PromotionLogicBase
@property (readonly) NSString *calculatedDiscount __attribute__((swift_name("calculatedDiscount")));
@property (readonly) NSString *eligibilityStatus __attribute__((swift_name("eligibilityStatus")));
@property (readonly) NSString *objectId __attribute__((swift_name("objectId")));
@property (readonly) NSString *objectType __attribute__((swift_name("objectType")));
@property (readonly) NSArray<NSString *> *tags __attribute__((swift_name("tags")));
@property (readonly) BOOL valid __attribute__((swift_name("valid")));
@property (readonly) NSArray<NSString *> *validationMessages __attribute__((swift_name("validationMessages")));
- (instancetype)initWithObjectId:(NSString *)objectId objectType:(NSString *)objectType valid:(BOOL)valid calculatedDiscount:(NSString *)calculatedDiscount eligibilityStatus:(NSString *)eligibilityStatus tags:(NSArray<NSString *> *)tags validationMessages:(NSArray<NSString *> *)validationMessages __attribute__((swift_name("init(objectId:objectType:valid:calculatedDiscount:eligibilityStatus:tags:validationMessages:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicDiscountItemResult *)doCopyObjectId:(NSString *)objectId objectType:(NSString *)objectType valid:(BOOL)valid calculatedDiscount:(NSString *)calculatedDiscount eligibilityStatus:(NSString *)eligibilityStatus tags:(NSArray<NSString *> *)tags validationMessages:(NSArray<NSString *> *)validationMessages __attribute__((swift_name("doCopy(objectId:objectType:valid:calculatedDiscount:eligibilityStatus:tags:validationMessages:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ValidateDiscountsRequest")))
@interface PromotionLogicValidateDiscountsRequest : PromotionLogicBase
@property (readonly) NSArray<PromotionLogicDiscountItemRequest *> *items __attribute__((swift_name("items")));
@property (readonly) NSString *orderId __attribute__((swift_name("orderId")));
@property (readonly) NSString *orderValue __attribute__((swift_name("orderValue")));
- (instancetype)initWithOrderId:(NSString *)orderId orderValue:(NSString *)orderValue items:(NSArray<PromotionLogicDiscountItemRequest *> *)items __attribute__((swift_name("init(orderId:orderValue:items:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicValidateDiscountsRequest *)doCopyOrderId:(NSString *)orderId orderValue:(NSString *)orderValue items:(NSArray<PromotionLogicDiscountItemRequest *> *)items __attribute__((swift_name("doCopy(orderId:orderValue:items:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ValidateDiscountsResult")))
@interface PromotionLogicValidateDiscountsResult : PromotionLogicBase
@property (readonly) NSArray<NSString *> *businessRuleViolations __attribute__((swift_name("businessRuleViolations")));
@property (readonly) NSString *finalAmount __attribute__((swift_name("finalAmount")));
@property (readonly) NSArray<PromotionLogicDiscountItemResult *> *invalidItems __attribute__((swift_name("invalidItems")));
@property (readonly) NSArray<PromotionLogicDiscountItemResult *> *items __attribute__((swift_name("items")));
@property (readonly) BOOL overallValid __attribute__((swift_name("overallValid")));
@property (readonly) NSString *totalDiscountAmount __attribute__((swift_name("totalDiscountAmount")));
@property (readonly) NSArray<PromotionLogicDiscountItemResult *> *validItems __attribute__((swift_name("validItems")));
- (instancetype)initWithOverallValid:(BOOL)overallValid totalDiscountAmount:(NSString *)totalDiscountAmount finalAmount:(NSString *)finalAmount items:(NSArray<PromotionLogicDiscountItemResult *> *)items businessRuleViolations:(NSArray<NSString *> *)businessRuleViolations __attribute__((swift_name("init(overallValid:totalDiscountAmount:finalAmount:items:businessRuleViolations:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicValidateDiscountsResult *)doCopyOverallValid:(BOOL)overallValid totalDiscountAmount:(NSString *)totalDiscountAmount finalAmount:(NSString *)finalAmount items:(NSArray<PromotionLogicDiscountItemResult *> *)items businessRuleViolations:(NSArray<NSString *> *)businessRuleViolations __attribute__((swift_name("doCopy(overallValid:totalDiscountAmount:finalAmount:items:businessRuleViolations:)")));
- (NSString *)discountForObjectId:(NSString *)objectId __attribute__((swift_name("discountFor(objectId:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (BOOL)isValidForObjectId:(NSString *)objectId __attribute__((swift_name("isValidFor(objectId:)")));
- (PromotionLogicDiscountItemResult * _Nullable)itemForObjectId:(NSString *)objectId __attribute__((swift_name("itemFor(objectId:)")));
- (NSArray<NSString *> *)reasonForObjectId:(NSString *)objectId __attribute__((swift_name("reasonFor(objectId:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ApplicableProduct")))
@interface PromotionLogicApplicableProduct : PromotionLogicBase
@property (readonly) NSString * _Nullable image __attribute__((swift_name("image")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) NSString *productId __attribute__((swift_name("productId")));
@property (readonly) NSString * _Nullable sku __attribute__((swift_name("sku")));
@property (readonly) NSString *type __attribute__((swift_name("type")));
- (instancetype)initWithProductId:(NSString *)productId sku:(NSString * _Nullable)sku name:(NSString *)name image:(NSString * _Nullable)image type:(NSString *)type __attribute__((swift_name("init(productId:sku:name:image:type:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicApplicableProduct *)doCopyProductId:(NSString *)productId sku:(NSString * _Nullable)sku name:(NSString *)name image:(NSString * _Nullable)image type:(NSString *)type __attribute__((swift_name("doCopy(productId:sku:name:image:type:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SearchCustomerVouchersRequest")))
@interface PromotionLogicSearchCustomerVouchersRequest : PromotionLogicBase
@property (readonly) NSString * _Nullable keyword __attribute__((swift_name("keyword")));
@property (readonly) PromotionLogicInt * _Nullable page __attribute__((swift_name("page")));
@property (readonly) NSString * _Nullable serviceCode __attribute__((swift_name("serviceCode")));
@property (readonly) PromotionLogicInt * _Nullable size __attribute__((swift_name("size")));
@property (readonly) NSString * _Nullable tab __attribute__((swift_name("tab")));
- (instancetype)initWithKeyword:(NSString * _Nullable)keyword serviceCode:(NSString * _Nullable)serviceCode tab:(NSString * _Nullable)tab page:(PromotionLogicInt * _Nullable)page size:(PromotionLogicInt * _Nullable)size __attribute__((swift_name("init(keyword:serviceCode:tab:page:size:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicSearchCustomerVouchersRequest *)doCopyKeyword:(NSString * _Nullable)keyword serviceCode:(NSString * _Nullable)serviceCode tab:(NSString * _Nullable)tab page:(PromotionLogicInt * _Nullable)page size:(PromotionLogicInt * _Nullable)size __attribute__((swift_name("doCopy(keyword:serviceCode:tab:page:size:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SearchCustomerVouchersResult")))
@interface PromotionLogicSearchCustomerVouchersResult : PromotionLogicBase
@property (readonly) NSArray<PromotionLogicVoucherItem *> *content __attribute__((swift_name("content")));
@property (readonly) NSString * _Nullable defaultTab __attribute__((swift_name("defaultTab")));
@property (readonly) PromotionLogicInt * _Nullable expireWarningDate __attribute__((swift_name("expireWarningDate")));
@property (readonly) NSString * _Nullable keyword __attribute__((swift_name("keyword")));
@property (readonly) PromotionLogicBoolean * _Nullable last __attribute__((swift_name("last")));
@property (readonly) PromotionLogicInt * _Nullable number __attribute__((swift_name("number")));
@property (readonly) NSString * _Nullable selectedTab __attribute__((swift_name("selectedTab")));
@property (readonly) NSString * _Nullable serviceCode __attribute__((swift_name("serviceCode")));
@property (readonly) PromotionLogicInt * _Nullable size __attribute__((swift_name("size")));
@property (readonly) NSArray<PromotionLogicVoucherTabItem *> *tabs __attribute__((swift_name("tabs")));
@property (readonly) PromotionLogicLong * _Nullable totalElements __attribute__((swift_name("totalElements")));
- (instancetype)initWithKeyword:(NSString * _Nullable)keyword serviceCode:(NSString * _Nullable)serviceCode tabs:(NSArray<PromotionLogicVoucherTabItem *> *)tabs defaultTab:(NSString * _Nullable)defaultTab selectedTab:(NSString * _Nullable)selectedTab content:(NSArray<PromotionLogicVoucherItem *> *)content number:(PromotionLogicInt * _Nullable)number size:(PromotionLogicInt * _Nullable)size last:(PromotionLogicBoolean * _Nullable)last totalElements:(PromotionLogicLong * _Nullable)totalElements expireWarningDate:(PromotionLogicInt * _Nullable)expireWarningDate __attribute__((swift_name("init(keyword:serviceCode:tabs:defaultTab:selectedTab:content:number:size:last:totalElements:expireWarningDate:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicSearchCustomerVouchersResult *)doCopyKeyword:(NSString * _Nullable)keyword serviceCode:(NSString * _Nullable)serviceCode tabs:(NSArray<PromotionLogicVoucherTabItem *> *)tabs defaultTab:(NSString * _Nullable)defaultTab selectedTab:(NSString * _Nullable)selectedTab content:(NSArray<PromotionLogicVoucherItem *> *)content number:(PromotionLogicInt * _Nullable)number size:(PromotionLogicInt * _Nullable)size last:(PromotionLogicBoolean * _Nullable)last totalElements:(PromotionLogicLong * _Nullable)totalElements expireWarningDate:(PromotionLogicInt * _Nullable)expireWarningDate __attribute__((swift_name("doCopy(keyword:serviceCode:tabs:defaultTab:selectedTab:content:number:size:last:totalElements:expireWarningDate:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString * _Nullable)resolveActiveTabRequestedTab:(NSString * _Nullable)requestedTab __attribute__((swift_name("resolveActiveTab(requestedTab:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VoucherDetail")))
@interface PromotionLogicVoucherDetail : PromotionLogicBase
@property (readonly) NSArray<PromotionLogicApplicableProduct *> *applicableProducts __attribute__((swift_name("applicableProducts")));
@property (readonly) NSString * _Nullable banner __attribute__((swift_name("banner")));
@property (readonly) NSString * _Nullable campaignId __attribute__((swift_name("campaignId")));
@property (readonly) NSString * _Nullable campaignStatus __attribute__((swift_name("campaignStatus")));
@property (readonly) NSString * _Nullable campaignType __attribute__((swift_name("campaignType")));
@property (readonly) NSArray<NSString *> *codes __attribute__((swift_name("codes")));
@property (readonly) NSString * _Nullable description_ __attribute__((swift_name("description_")));
@property (readonly) NSString * _Nullable displayStatusLabel __attribute__((swift_name("displayStatusLabel")));
@property (readonly) NSString * _Nullable expirationDate __attribute__((swift_name("expirationDate")));
@property (readonly) PromotionLogicInt * _Nullable expireWarningDate __attribute__((swift_name("expireWarningDate")));
@property (readonly) NSString * _Nullable guideline __attribute__((swift_name("guideline")));
@property (readonly) NSString * _Nullable logo __attribute__((swift_name("logo")));
@property (readonly) NSString * _Nullable merchantName __attribute__((swift_name("merchantName")));
@property (readonly) NSString * _Nullable startDate __attribute__((swift_name("startDate")));
@property (readonly) NSString * _Nullable status __attribute__((swift_name("status")));
@property (readonly) NSString * _Nullable title __attribute__((swift_name("title")));
@property (readonly) NSString * _Nullable usageGuideUrl __attribute__((swift_name("usageGuideUrl")));
@property (readonly) NSString *voucherId __attribute__((swift_name("voucherId")));
- (instancetype)initWithVoucherId:(NSString *)voucherId merchantName:(NSString * _Nullable)merchantName logo:(NSString * _Nullable)logo banner:(NSString * _Nullable)banner title:(NSString * _Nullable)title description:(NSString * _Nullable)description guideline:(NSString * _Nullable)guideline startDate:(NSString * _Nullable)startDate expirationDate:(NSString * _Nullable)expirationDate status:(NSString * _Nullable)status displayStatusLabel:(NSString * _Nullable)displayStatusLabel campaignId:(NSString * _Nullable)campaignId campaignType:(NSString * _Nullable)campaignType campaignStatus:(NSString * _Nullable)campaignStatus applicableProducts:(NSArray<PromotionLogicApplicableProduct *> *)applicableProducts expireWarningDate:(PromotionLogicInt * _Nullable)expireWarningDate codes:(NSArray<NSString *> *)codes usageGuideUrl:(NSString * _Nullable)usageGuideUrl __attribute__((swift_name("init(voucherId:merchantName:logo:banner:title:description:guideline:startDate:expirationDate:status:displayStatusLabel:campaignId:campaignType:campaignStatus:applicableProducts:expireWarningDate:codes:usageGuideUrl:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicVoucherDetail *)doCopyVoucherId:(NSString *)voucherId merchantName:(NSString * _Nullable)merchantName logo:(NSString * _Nullable)logo banner:(NSString * _Nullable)banner title:(NSString * _Nullable)title description:(NSString * _Nullable)description guideline:(NSString * _Nullable)guideline startDate:(NSString * _Nullable)startDate expirationDate:(NSString * _Nullable)expirationDate status:(NSString * _Nullable)status displayStatusLabel:(NSString * _Nullable)displayStatusLabel campaignId:(NSString * _Nullable)campaignId campaignType:(NSString * _Nullable)campaignType campaignStatus:(NSString * _Nullable)campaignStatus applicableProducts:(NSArray<PromotionLogicApplicableProduct *> *)applicableProducts expireWarningDate:(PromotionLogicInt * _Nullable)expireWarningDate codes:(NSArray<NSString *> *)codes usageGuideUrl:(NSString * _Nullable)usageGuideUrl __attribute__((swift_name("doCopy(voucherId:merchantName:logo:banner:title:description:guideline:startDate:expirationDate:status:displayStatusLabel:campaignId:campaignType:campaignStatus:applicableProducts:expireWarningDate:codes:usageGuideUrl:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VoucherDisplayState")))
@interface PromotionLogicVoucherDisplayState : PromotionLogicKotlinEnum<PromotionLogicVoucherDisplayState *>
@property (class, readonly) PromotionLogicVoucherDisplayState *usable __attribute__((swift_name("usable")));
@property (class, readonly) PromotionLogicVoucherDisplayState *used __attribute__((swift_name("used")));
@property (class, readonly) PromotionLogicVoucherDisplayState *expired __attribute__((swift_name("expired")));
@property (class, readonly) PromotionLogicVoucherDisplayState *ineligible __attribute__((swift_name("ineligible")));
@property (class, readonly) NSArray<PromotionLogicVoucherDisplayState *> *entries __attribute__((swift_name("entries")));
@property (readonly) BOOL isUsable __attribute__((swift_name("isUsable")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (PromotionLogicKotlinArray<PromotionLogicVoucherDisplayState *> *)values __attribute__((swift_name("values()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VoucherItem")))
@interface PromotionLogicVoucherItem : PromotionLogicBase
@property (readonly) NSArray<PromotionLogicApplicableProduct *> *applicableProducts __attribute__((swift_name("applicableProducts")));
@property (readonly) NSString * _Nullable campaignId __attribute__((swift_name("campaignId")));
@property (readonly) NSString * _Nullable campaignType __attribute__((swift_name("campaignType")));
@property (readonly) NSString * _Nullable description_ __attribute__((swift_name("description_")));
@property (readonly) NSString * _Nullable displayStatusLabel __attribute__((swift_name("displayStatusLabel")));
@property (readonly) NSString * _Nullable expirationDate __attribute__((swift_name("expirationDate")));
@property (readonly) BOOL isAutoApplied __attribute__((swift_name("isAutoApplied")));
@property (readonly) NSString * _Nullable logo __attribute__((swift_name("logo")));
@property (readonly) NSString * _Nullable merchantName __attribute__((swift_name("merchantName")));
@property (readonly) NSString *objectType __attribute__((swift_name("objectType")));
@property (readonly) NSString * _Nullable status __attribute__((swift_name("status")));
@property (readonly) NSString * _Nullable title __attribute__((swift_name("title")));
@property (readonly) NSString *voucherId __attribute__((swift_name("voucherId")));
- (instancetype)initWithVoucherId:(NSString *)voucherId merchantName:(NSString * _Nullable)merchantName title:(NSString * _Nullable)title description:(NSString * _Nullable)description logo:(NSString * _Nullable)logo expirationDate:(NSString * _Nullable)expirationDate status:(NSString * _Nullable)status displayStatusLabel:(NSString * _Nullable)displayStatusLabel campaignId:(NSString * _Nullable)campaignId campaignType:(NSString * _Nullable)campaignType objectType:(NSString *)objectType isAutoApplied:(BOOL)isAutoApplied applicableProducts:(NSArray<PromotionLogicApplicableProduct *> *)applicableProducts __attribute__((swift_name("init(voucherId:merchantName:title:description:logo:expirationDate:status:displayStatusLabel:campaignId:campaignType:objectType:isAutoApplied:applicableProducts:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicVoucherItem *)doCopyVoucherId:(NSString *)voucherId merchantName:(NSString * _Nullable)merchantName title:(NSString * _Nullable)title description:(NSString * _Nullable)description logo:(NSString * _Nullable)logo expirationDate:(NSString * _Nullable)expirationDate status:(NSString * _Nullable)status displayStatusLabel:(NSString * _Nullable)displayStatusLabel campaignId:(NSString * _Nullable)campaignId campaignType:(NSString * _Nullable)campaignType objectType:(NSString *)objectType isAutoApplied:(BOOL)isAutoApplied applicableProducts:(NSArray<PromotionLogicApplicableProduct *> *)applicableProducts __attribute__((swift_name("doCopy(voucherId:merchantName:title:description:logo:expirationDate:status:displayStatusLabel:campaignId:campaignType:objectType:isAutoApplied:applicableProducts:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VoucherStatus")))
@interface PromotionLogicVoucherStatus : PromotionLogicKotlinEnum<PromotionLogicVoucherStatus *>
@property (class, readonly, getter=companion) PromotionLogicVoucherStatusCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) PromotionLogicVoucherStatus *active __attribute__((swift_name("active")));
@property (class, readonly) PromotionLogicVoucherStatus *available __attribute__((swift_name("available")));
@property (class, readonly) PromotionLogicVoucherStatus *usable __attribute__((swift_name("usable")));
@property (class, readonly) PromotionLogicVoucherStatus *availableToClaim __attribute__((swift_name("availableToClaim")));
@property (class, readonly) PromotionLogicVoucherStatus *redeemed __attribute__((swift_name("redeemed")));
@property (class, readonly) PromotionLogicVoucherStatus *used __attribute__((swift_name("used")));
@property (class, readonly) PromotionLogicVoucherStatus *expired __attribute__((swift_name("expired")));
@property (class, readonly) PromotionLogicVoucherStatus *reserved __attribute__((swift_name("reserved")));
@property (class, readonly) PromotionLogicVoucherStatus *revoked __attribute__((swift_name("revoked")));
@property (class, readonly) PromotionLogicVoucherStatus *suspended __attribute__((swift_name("suspended")));
@property (class, readonly) PromotionLogicVoucherStatus *ineligible __attribute__((swift_name("ineligible")));
@property (class, readonly) PromotionLogicVoucherStatus *notEligible __attribute__((swift_name("notEligible")));
@property (class, readonly) PromotionLogicVoucherStatus *unknown __attribute__((swift_name("unknown")));
@property (class, readonly) NSArray<PromotionLogicVoucherStatus *> *entries __attribute__((swift_name("entries")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (PromotionLogicKotlinArray<PromotionLogicVoucherStatus *> *)values __attribute__((swift_name("values()")));
- (PromotionLogicVoucherDisplayState *)displayState __attribute__((swift_name("displayState()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VoucherStatus.Companion")))
@interface PromotionLogicVoucherStatusCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicVoucherStatusCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (PromotionLogicVoucherStatus *)fromRaw:(NSString * _Nullable)raw __attribute__((swift_name("from(raw:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VoucherTabItem")))
@interface PromotionLogicVoucherTabItem : PromotionLogicBase
@property (readonly) NSString *code __attribute__((swift_name("code")));
@property (readonly) PromotionLogicInt * _Nullable count __attribute__((swift_name("count")));
@property (readonly) BOOL isDefault __attribute__((swift_name("isDefault")));
@property (readonly) NSString *label __attribute__((swift_name("label")));
@property (readonly) PromotionLogicInt * _Nullable order __attribute__((swift_name("order")));
- (instancetype)initWithCode:(NSString *)code label:(NSString *)label count:(PromotionLogicInt * _Nullable)count order:(PromotionLogicInt * _Nullable)order isDefault:(BOOL)isDefault __attribute__((swift_name("init(code:label:count:order:isDefault:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicVoucherTabItem *)doCopyCode:(NSString *)code label:(NSString *)label count:(PromotionLogicInt * _Nullable)count order:(PromotionLogicInt * _Nullable)order isDefault:(BOOL)isDefault __attribute__((swift_name("doCopy(code:label:count:order:isDefault:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CreateRedemptionSessionUseCase")))
@interface PromotionLogicCreateRedemptionSessionUseCase : PromotionLogicBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
/**
 * @note This method converts instances of PromotionException, NetworkException, CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeRequest:(PromotionLogicCreateRedemptionRequest *)request completionHandler:(void (^)(PromotionLogicCreateRedemptionResult * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(request:completionHandler:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FetchFeatureFlagsUseCase")))
@interface PromotionLogicFetchFeatureFlagsUseCase : PromotionLogicBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(completionHandler:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FindEligibleCampaignsUseCase")))
@interface PromotionLogicFindEligibleCampaignsUseCase : PromotionLogicBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
/**
 * @note This method converts instances of PromotionException, NetworkException, CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeRequest:(PromotionLogicFindEligibleCampaignsRequest *)request completionHandler:(void (^)(PromotionLogicEligibleOffersResult * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(request:completionHandler:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GetCustomerVoucherDetailUseCase")))
@interface PromotionLogicGetCustomerVoucherDetailUseCase : PromotionLogicBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
/**
 * @note This method converts instances of PromotionException, NetworkException, CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeVoucherId:(NSString *)voucherId service:(NSString * _Nullable)service completionHandler:(void (^)(PromotionLogicVoucherDetail * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(voucherId:service:completionHandler:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GetFeatureFlagsUseCase")))
@interface PromotionLogicGetFeatureFlagsUseCase : PromotionLogicBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSArray<PromotionLogicFeatureFlag *> *)invokeFeatureNames:(NSArray<NSString *> *)featureNames __attribute__((swift_name("invoke(featureNames:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GetPromotionFeatureFlagsUseCase")))
@interface PromotionLogicGetPromotionFeatureFlagsUseCase : PromotionLogicBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (PromotionLogicPromotionFeatureFlags *)invoke __attribute__((swift_name("invoke()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IsFeatureEnabledUseCase")))
@interface PromotionLogicIsFeatureEnabledUseCase : PromotionLogicBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (BOOL)invokeFeatureName:(NSString *)featureName __attribute__((swift_name("invoke(featureName:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PromotionFeatureFlagUseCases")))
@interface PromotionLogicPromotionFeatureFlagUseCases : PromotionLogicBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (PromotionLogicPromotionFeatureFlags *)all __attribute__((swift_name("all()")));
- (NSArray<PromotionLogicFeatureFlag *> *)flagsOfFeatureNames:(NSArray<NSString *> *)featureNames __attribute__((swift_name("flagsOf(featureNames:)")));
- (BOOL)isEnabledFeatureName:(NSString *)featureName __attribute__((swift_name("isEnabled(featureName:)")));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)refreshWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("refresh(completionHandler:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PromotionFeatureGate")))
@interface PromotionLogicPromotionFeatureGate : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicPromotionFeatureGate *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)promotionFeatureGate __attribute__((swift_name("init()")));
- (BOOL)canApplyVoucher __attribute__((swift_name("canApplyVoucher()")));
- (BOOL)canOpenVoucherDetail __attribute__((swift_name("canOpenVoucherDetail()")));
- (BOOL)canOpenVoucherList __attribute__((swift_name("canOpenVoucherList()")));
- (BOOL)canRedeemVoucher __attribute__((swift_name("canRedeemVoucher()")));
- (BOOL)canShowVoucherSelection __attribute__((swift_name("canShowVoucherSelection()")));
- (BOOL)isEnabledFlagName:(NSString *)flagName __attribute__((swift_name("isEnabled(flagName:)")));
- (BOOL)isSdkEnabled __attribute__((swift_name("isSdkEnabled()")));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)refreshWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("refresh(completionHandler:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PromotionUseCases")))
@interface PromotionLogicPromotionUseCases : PromotionLogicBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)createRedemptionRequest:(PromotionLogicCreateRedemptionRequest *)request completionHandler:(void (^)(id<PromotionLogicPromotionResult> _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("createRedemption(request:completionHandler:)")));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)findEligibleRequest:(PromotionLogicFindEligibleCampaignsRequest *)request completionHandler:(void (^)(id<PromotionLogicPromotionResult> _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("findEligible(request:completionHandler:)")));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getVoucherDetailVoucherId:(NSString *)voucherId service:(NSString * _Nullable)service completionHandler:(void (^)(id<PromotionLogicPromotionResult> _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getVoucherDetail(voucherId:service:completionHandler:)")));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)searchVouchersRequest:(PromotionLogicSearchCustomerVouchersRequest *)request completionHandler:(void (^)(id<PromotionLogicPromotionResult> _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("searchVouchers(request:completionHandler:)")));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)validateDiscountsRequest:(PromotionLogicValidateDiscountsRequest *)request completionHandler:(void (^)(id<PromotionLogicPromotionResult> _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("validateDiscounts(request:completionHandler:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SearchCustomerVouchersUseCase")))
@interface PromotionLogicSearchCustomerVouchersUseCase : PromotionLogicBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
/**
 * @note This method converts instances of PromotionException, NetworkException, CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeRequest:(PromotionLogicSearchCustomerVouchersRequest *)request completionHandler:(void (^)(PromotionLogicSearchCustomerVouchersResult * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(request:completionHandler:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ValidateStackableDiscountsUseCase")))
@interface PromotionLogicValidateStackableDiscountsUseCase : PromotionLogicBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
/**
 * @note This method converts instances of PromotionException, NetworkException, CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeRequest:(PromotionLogicValidateDiscountsRequest *)request completionHandler:(void (^)(PromotionLogicValidateDiscountsResult * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(request:completionHandler:)")));
@end
__attribute__((swift_name("PRMEffect")))
@protocol PromotionLogicPRMEffect
@required
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PRMEffectShowError")))
@interface PromotionLogicPRMEffectShowError : PromotionLogicBase <PromotionLogicPRMEffect>
@property (readonly) NSString *errorCode __attribute__((swift_name("errorCode")));
- (instancetype)initWithErrorCode:(NSString *)errorCode __attribute__((swift_name("init(errorCode:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicPRMEffectShowError *)doCopyErrorCode:(NSString *)errorCode __attribute__((swift_name("doCopy(errorCode:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((swift_name("PRMStore")))
@protocol PromotionLogicPRMStore
@required
- (void)dispatchIntent:(id)intent __attribute__((swift_name("dispatch(intent:)")));
- (NSString * _Nullable)errorOfState:(id)state __attribute__((swift_name("errorOf(state:)")));
@property (readonly) id consumeErrorIntent __attribute__((swift_name("consumeErrorIntent")));
@property (readonly) id<PromotionLogicKotlinx_coroutines_coreFlow> effects __attribute__((swift_name("effects")));
@property (readonly) id<PromotionLogicKotlinx_coroutines_coreStateFlow> state __attribute__((swift_name("state")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PromotionCancellable")))
@interface PromotionLogicPromotionCancellable : PromotionLogicBase
- (instancetype)initWithOnCancel:(void (^)(void))onCancel __attribute__((swift_name("init(onCancel:)"))) __attribute__((objc_designated_initializer));
- (void)cancel __attribute__((swift_name("cancel()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChooseOffer")))
@interface PromotionLogicChooseOffer : PromotionLogicBase
@property (readonly) PromotionLogicInt * _Nullable expiringInDays __attribute__((swift_name("expiringInDays")));
@property (readonly) BOOL isExpired __attribute__((swift_name("isExpired")));
@property (readonly) BOOL isRejected __attribute__((swift_name("isRejected")));
@property (readonly) BOOL isUsable __attribute__((swift_name("isUsable")));
@property (readonly) PromotionLogicEligibleOffer *source __attribute__((swift_name("source")));
- (instancetype)initWithSource:(PromotionLogicEligibleOffer *)source isUsable:(BOOL)isUsable expiringInDays:(PromotionLogicInt * _Nullable)expiringInDays isExpired:(BOOL)isExpired isRejected:(BOOL)isRejected __attribute__((swift_name("init(source:isUsable:expiringInDays:isExpired:isRejected:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicChooseOffer *)doCopySource:(PromotionLogicEligibleOffer *)source isUsable:(BOOL)isUsable expiringInDays:(PromotionLogicInt * _Nullable)expiringInDays isExpired:(BOOL)isExpired isRejected:(BOOL)isRejected __attribute__((swift_name("doCopy(source:isUsable:expiringInDays:isExpired:isRejected:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((swift_name("ChoosePromotionIntent")))
@protocol PromotionLogicChoosePromotionIntent
@required
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoosePromotionIntentApplyFinished")))
@interface PromotionLogicChoosePromotionIntentApplyFinished : PromotionLogicBase <PromotionLogicChoosePromotionIntent>
@property (class, readonly, getter=shared) PromotionLogicChoosePromotionIntentApplyFinished *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)applyFinished __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoosePromotionIntentApplyRejected")))
@interface PromotionLogicChoosePromotionIntentApplyRejected : PromotionLogicBase <PromotionLogicChoosePromotionIntent>
@property (readonly) NSArray<PromotionLogicRejectedOffer *> *items __attribute__((swift_name("items")));
- (instancetype)initWithItems:(NSArray<PromotionLogicRejectedOffer *> *)items __attribute__((swift_name("init(items:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicChoosePromotionIntentApplyRejected *)doCopyItems:(NSArray<PromotionLogicRejectedOffer *> *)items __attribute__((swift_name("doCopy(items:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoosePromotionIntentApplyStarted")))
@interface PromotionLogicChoosePromotionIntentApplyStarted : PromotionLogicBase <PromotionLogicChoosePromotionIntent>
@property (class, readonly, getter=shared) PromotionLogicChoosePromotionIntentApplyStarted *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)applyStarted __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoosePromotionIntentClearKeyword")))
@interface PromotionLogicChoosePromotionIntentClearKeyword : PromotionLogicBase <PromotionLogicChoosePromotionIntent>
@property (class, readonly, getter=shared) PromotionLogicChoosePromotionIntentClearKeyword *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)clearKeyword __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoosePromotionIntentConsumeApplyMessage")))
@interface PromotionLogicChoosePromotionIntentConsumeApplyMessage : PromotionLogicBase <PromotionLogicChoosePromotionIntent>
@property (class, readonly, getter=shared) PromotionLogicChoosePromotionIntentConsumeApplyMessage *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)consumeApplyMessage __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoosePromotionIntentConsumeError")))
@interface PromotionLogicChoosePromotionIntentConsumeError : PromotionLogicBase <PromotionLogicChoosePromotionIntent>
@property (class, readonly, getter=shared) PromotionLogicChoosePromotionIntentConsumeError *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)consumeError __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoosePromotionIntentLoadInitial")))
@interface PromotionLogicChoosePromotionIntentLoadInitial : PromotionLogicBase <PromotionLogicChoosePromotionIntent>
@property (class, readonly, getter=shared) PromotionLogicChoosePromotionIntentLoadInitial *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)loadInitial __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoosePromotionIntentLoadMoreMyVouchers")))
@interface PromotionLogicChoosePromotionIntentLoadMoreMyVouchers : PromotionLogicBase <PromotionLogicChoosePromotionIntent>
@property (class, readonly, getter=shared) PromotionLogicChoosePromotionIntentLoadMoreMyVouchers *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)loadMoreMyVouchers __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoosePromotionIntentLoadMoreOtherVouchers")))
@interface PromotionLogicChoosePromotionIntentLoadMoreOtherVouchers : PromotionLogicBase <PromotionLogicChoosePromotionIntent>
@property (class, readonly, getter=shared) PromotionLogicChoosePromotionIntentLoadMoreOtherVouchers *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)loadMoreOtherVouchers __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoosePromotionIntentPreload")))
@interface PromotionLogicChoosePromotionIntentPreload : PromotionLogicBase <PromotionLogicChoosePromotionIntent>
@property (readonly) BOOL myIsLastPage __attribute__((swift_name("myIsLastPage")));
@property (readonly) NSArray<PromotionLogicEligibleOffer *> *myOffers __attribute__((swift_name("myOffers")));
@property (readonly) BOOL otherIsLastPage __attribute__((swift_name("otherIsLastPage")));
@property (readonly) NSArray<PromotionLogicEligibleOffer *> *otherOffers __attribute__((swift_name("otherOffers")));
- (instancetype)initWithMyOffers:(NSArray<PromotionLogicEligibleOffer *> *)myOffers otherOffers:(NSArray<PromotionLogicEligibleOffer *> *)otherOffers myIsLastPage:(BOOL)myIsLastPage otherIsLastPage:(BOOL)otherIsLastPage __attribute__((swift_name("init(myOffers:otherOffers:myIsLastPage:otherIsLastPage:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicChoosePromotionIntentPreload *)doCopyMyOffers:(NSArray<PromotionLogicEligibleOffer *> *)myOffers otherOffers:(NSArray<PromotionLogicEligibleOffer *> *)otherOffers myIsLastPage:(BOOL)myIsLastPage otherIsLastPage:(BOOL)otherIsLastPage __attribute__((swift_name("doCopy(myOffers:otherOffers:myIsLastPage:otherIsLastPage:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoosePromotionIntentQueryChanged")))
@interface PromotionLogicChoosePromotionIntentQueryChanged : PromotionLogicBase <PromotionLogicChoosePromotionIntent>
@property (readonly) NSString *keyword __attribute__((swift_name("keyword")));
- (instancetype)initWithKeyword:(NSString *)keyword __attribute__((swift_name("init(keyword:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicChoosePromotionIntentQueryChanged *)doCopyKeyword:(NSString *)keyword __attribute__((swift_name("doCopy(keyword:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoosePromotionIntentRefresh")))
@interface PromotionLogicChoosePromotionIntentRefresh : PromotionLogicBase <PromotionLogicChoosePromotionIntent>
@property (class, readonly, getter=shared) PromotionLogicChoosePromotionIntentRefresh *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)refresh __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoosePromotionIntentSearch")))
@interface PromotionLogicChoosePromotionIntentSearch : PromotionLogicBase <PromotionLogicChoosePromotionIntent>
@property (class, readonly, getter=shared) PromotionLogicChoosePromotionIntentSearch *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)search __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoosePromotionIntentSeeMoreMy")))
@interface PromotionLogicChoosePromotionIntentSeeMoreMy : PromotionLogicBase <PromotionLogicChoosePromotionIntent>
@property (class, readonly, getter=shared) PromotionLogicChoosePromotionIntentSeeMoreMy *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)seeMoreMy __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoosePromotionIntentSeedOnce")))
@interface PromotionLogicChoosePromotionIntentSeedOnce : PromotionLogicBase <PromotionLogicChoosePromotionIntent>
@property (readonly) NSArray<NSString *> *preSelectedIds __attribute__((swift_name("preSelectedIds")));
- (instancetype)initWithPreSelectedIds:(NSArray<NSString *> *)preSelectedIds __attribute__((swift_name("init(preSelectedIds:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicChoosePromotionIntentSeedOnce *)doCopyPreSelectedIds:(NSArray<NSString *> *)preSelectedIds __attribute__((swift_name("doCopy(preSelectedIds:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoosePromotionIntentSetPreSelected")))
@interface PromotionLogicChoosePromotionIntentSetPreSelected : PromotionLogicBase <PromotionLogicChoosePromotionIntent>
@property (readonly) NSArray<NSString *> *ids __attribute__((swift_name("ids")));
- (instancetype)initWithIds:(NSArray<NSString *> *)ids __attribute__((swift_name("init(ids:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicChoosePromotionIntentSetPreSelected *)doCopyIds:(NSArray<NSString *> *)ids __attribute__((swift_name("doCopy(ids:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoosePromotionIntentToggleSelection")))
@interface PromotionLogicChoosePromotionIntentToggleSelection : PromotionLogicBase <PromotionLogicChoosePromotionIntent>
@property (readonly) NSString *id __attribute__((swift_name("id")));
- (instancetype)initWithId:(NSString *)id __attribute__((swift_name("init(id:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicChoosePromotionIntentToggleSelection *)doCopyId:(NSString *)id __attribute__((swift_name("doCopy(id:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoosePromotionState")))
@interface PromotionLogicChoosePromotionState : PromotionLogicBase
@property (readonly) NSString * _Nullable applyMessage __attribute__((swift_name("applyMessage")));
@property (readonly) NSString * _Nullable errorCode __attribute__((swift_name("errorCode")));
@property (readonly) PromotionLogicInt * _Nullable expireWarningDate __attribute__((swift_name("expireWarningDate")));
@property (readonly) BOOL hasLoadedInitial __attribute__((swift_name("hasLoadedInitial")));
@property (readonly) BOOL isApplying __attribute__((swift_name("isApplying")));
@property (readonly) BOOL isEmpty __attribute__((swift_name("isEmpty")));
@property (readonly) BOOL isLoading __attribute__((swift_name("isLoading")));
@property (readonly) BOOL isLoadingMore __attribute__((swift_name("isLoadingMore")));
@property (readonly) BOOL isLoadingMoreOther __attribute__((swift_name("isLoadingMoreOther")));
@property (readonly) BOOL isMultiSelection __attribute__((swift_name("isMultiSelection")));
@property (readonly) BOOL isRefreshing __attribute__((swift_name("isRefreshing")));
@property (readonly) NSString *keyword __attribute__((swift_name("keyword")));
@property (readonly) BOOL loadFailed __attribute__((swift_name("loadFailed")));
@property (readonly) BOOL myExpanded __attribute__((swift_name("myExpanded")));
@property (readonly) BOOL myIsLastPage __attribute__((swift_name("myIsLastPage")));
@property (readonly) NSArray<PromotionLogicChooseOffer *> *myOffers __attribute__((swift_name("myOffers")));
@property (readonly) int32_t myPage __attribute__((swift_name("myPage")));
@property (readonly) int32_t mySize __attribute__((swift_name("mySize")));
@property (readonly) BOOL otherIsLastPage __attribute__((swift_name("otherIsLastPage")));
@property (readonly) NSArray<PromotionLogicChooseOffer *> *otherOffers __attribute__((swift_name("otherOffers")));
@property (readonly) int32_t otherPage __attribute__((swift_name("otherPage")));
@property (readonly) int32_t otherSize __attribute__((swift_name("otherSize")));
@property (readonly) NSArray<NSString *> *rejectedIds __attribute__((swift_name("rejectedIds")));
@property (readonly) NSArray<NSString *> *selectedIds __attribute__((swift_name("selectedIds")));
@property (readonly) NSString * _Nullable selectedTabCode __attribute__((swift_name("selectedTabCode")));
@property (readonly) NSArray<PromotionLogicMyPromotionTab *> *tabs __attribute__((swift_name("tabs")));
- (instancetype)initWithHasLoadedInitial:(BOOL)hasLoadedInitial isLoading:(BOOL)isLoading isRefreshing:(BOOL)isRefreshing isLoadingMore:(BOOL)isLoadingMore isLoadingMoreOther:(BOOL)isLoadingMoreOther isEmpty:(BOOL)isEmpty tabs:(NSArray<PromotionLogicMyPromotionTab *> *)tabs selectedTabCode:(NSString * _Nullable)selectedTabCode keyword:(NSString *)keyword myPage:(int32_t)myPage mySize:(int32_t)mySize myIsLastPage:(BOOL)myIsLastPage otherPage:(int32_t)otherPage otherSize:(int32_t)otherSize otherIsLastPage:(BOOL)otherIsLastPage myOffers:(NSArray<PromotionLogicChooseOffer *> *)myOffers otherOffers:(NSArray<PromotionLogicChooseOffer *> *)otherOffers expireWarningDate:(PromotionLogicInt * _Nullable)expireWarningDate isMultiSelection:(BOOL)isMultiSelection selectedIds:(NSArray<NSString *> *)selectedIds myExpanded:(BOOL)myExpanded errorCode:(NSString * _Nullable)errorCode loadFailed:(BOOL)loadFailed isApplying:(BOOL)isApplying rejectedIds:(NSArray<NSString *> *)rejectedIds applyMessage:(NSString * _Nullable)applyMessage __attribute__((swift_name("init(hasLoadedInitial:isLoading:isRefreshing:isLoadingMore:isLoadingMoreOther:isEmpty:tabs:selectedTabCode:keyword:myPage:mySize:myIsLastPage:otherPage:otherSize:otherIsLastPage:myOffers:otherOffers:expireWarningDate:isMultiSelection:selectedIds:myExpanded:errorCode:loadFailed:isApplying:rejectedIds:applyMessage:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicChoosePromotionState *)doCopyHasLoadedInitial:(BOOL)hasLoadedInitial isLoading:(BOOL)isLoading isRefreshing:(BOOL)isRefreshing isLoadingMore:(BOOL)isLoadingMore isLoadingMoreOther:(BOOL)isLoadingMoreOther isEmpty:(BOOL)isEmpty tabs:(NSArray<PromotionLogicMyPromotionTab *> *)tabs selectedTabCode:(NSString * _Nullable)selectedTabCode keyword:(NSString *)keyword myPage:(int32_t)myPage mySize:(int32_t)mySize myIsLastPage:(BOOL)myIsLastPage otherPage:(int32_t)otherPage otherSize:(int32_t)otherSize otherIsLastPage:(BOOL)otherIsLastPage myOffers:(NSArray<PromotionLogicChooseOffer *> *)myOffers otherOffers:(NSArray<PromotionLogicChooseOffer *> *)otherOffers expireWarningDate:(PromotionLogicInt * _Nullable)expireWarningDate isMultiSelection:(BOOL)isMultiSelection selectedIds:(NSArray<NSString *> *)selectedIds myExpanded:(BOOL)myExpanded errorCode:(NSString * _Nullable)errorCode loadFailed:(BOOL)loadFailed isApplying:(BOOL)isApplying rejectedIds:(NSArray<NSString *> *)rejectedIds applyMessage:(NSString * _Nullable)applyMessage __attribute__((swift_name("doCopy(hasLoadedInitial:isLoading:isRefreshing:isLoadingMore:isLoadingMoreOther:isEmpty:tabs:selectedTabCode:keyword:myPage:mySize:myIsLastPage:otherPage:otherSize:otherIsLastPage:myOffers:otherOffers:expireWarningDate:isMultiSelection:selectedIds:myExpanded:errorCode:loadFailed:isApplying:rejectedIds:applyMessage:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoosePromotionStore")))
@interface PromotionLogicChoosePromotionStore : PromotionLogicBase <PromotionLogicPRMStore>
@property (readonly) id<PromotionLogicChoosePromotionIntent> consumeErrorIntent __attribute__((swift_name("consumeErrorIntent")));
@property (readonly) id<PromotionLogicKotlinx_coroutines_coreStateFlow> state __attribute__((swift_name("state")));
- (instancetype)initWithFindEligibleCampaignsUseCase:(PromotionLogicFindEligibleCampaignsUseCase *)findEligibleCampaignsUseCase __attribute__((swift_name("init(findEligibleCampaignsUseCase:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithFindEligibleCampaignsUseCase:(PromotionLogicFindEligibleCampaignsUseCase *)findEligibleCampaignsUseCase scope:(id<PromotionLogicKotlinx_coroutines_coreCoroutineScope>)scope __attribute__((swift_name("init(findEligibleCampaignsUseCase:scope:)"))) __attribute__((objc_designated_initializer));
- (void)clear __attribute__((swift_name("clear()")));
- (PromotionLogicChoosePromotionState *)currentState __attribute__((swift_name("currentState()")));
- (void)dispatchIntent:(id<PromotionLogicChoosePromotionIntent>)intent __attribute__((swift_name("dispatch(intent:)")));
- (NSString * _Nullable)errorOfState:(PromotionLogicChoosePromotionState *)state __attribute__((swift_name("errorOf(state:)")));
- (PromotionLogicPromotionCancellable *)watchStateOnEach:(void (^)(PromotionLogicChoosePromotionState *))onEach __attribute__((swift_name("watchState(onEach:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChooseSeeMoreState")))
@interface PromotionLogicChooseSeeMoreState : PromotionLogicKotlinEnum<PromotionLogicChooseSeeMoreState *>
@property (class, readonly) PromotionLogicChooseSeeMoreState *hidden __attribute__((swift_name("hidden")));
@property (class, readonly) PromotionLogicChooseSeeMoreState *expand __attribute__((swift_name("expand")));
@property (class, readonly) PromotionLogicChooseSeeMoreState *collapse __attribute__((swift_name("collapse")));
@property (class, readonly) NSArray<PromotionLogicChooseSeeMoreState *> *entries __attribute__((swift_name("entries")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (PromotionLogicKotlinArray<PromotionLogicChooseSeeMoreState *> *)values __attribute__((swift_name("values()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RejectedOffer")))
@interface PromotionLogicRejectedOffer : PromotionLogicBase
@property (readonly) NSString *message __attribute__((swift_name("message")));
@property (readonly) NSString *objectId __attribute__((swift_name("objectId")));
- (instancetype)initWithObjectId:(NSString *)objectId message:(NSString *)message __attribute__((swift_name("init(objectId:message:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicRejectedOffer *)doCopyObjectId:(NSString *)objectId message:(NSString *)message __attribute__((swift_name("doCopy(objectId:message:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MyPromotionAction")))
@interface PromotionLogicMyPromotionAction : PromotionLogicKotlinEnum<PromotionLogicMyPromotionAction *>
@property (class, readonly) PromotionLogicMyPromotionAction *use __attribute__((swift_name("use")));
@property (class, readonly) PromotionLogicMyPromotionAction *none __attribute__((swift_name("none")));
@property (class, readonly) NSArray<PromotionLogicMyPromotionAction *> *entries __attribute__((swift_name("entries")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (PromotionLogicKotlinArray<PromotionLogicMyPromotionAction *> *)values __attribute__((swift_name("values()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MyPromotionBadge")))
@interface PromotionLogicMyPromotionBadge : PromotionLogicKotlinEnum<PromotionLogicMyPromotionBadge *>
@property (class, readonly) PromotionLogicMyPromotionBadge *none __attribute__((swift_name("none")));
@property (class, readonly) PromotionLogicMyPromotionBadge *expiringSoon __attribute__((swift_name("expiringSoon")));
@property (class, readonly) PromotionLogicMyPromotionBadge *used __attribute__((swift_name("used")));
@property (class, readonly) PromotionLogicMyPromotionBadge *expired __attribute__((swift_name("expired")));
@property (class, readonly) PromotionLogicMyPromotionBadge *ineligible __attribute__((swift_name("ineligible")));
@property (class, readonly) NSArray<PromotionLogicMyPromotionBadge *> *entries __attribute__((swift_name("entries")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (PromotionLogicKotlinArray<PromotionLogicMyPromotionBadge *> *)values __attribute__((swift_name("values()")));
@end
__attribute__((swift_name("MyPromotionIntent")))
@protocol PromotionLogicMyPromotionIntent
@required
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MyPromotionIntentConsumeError")))
@interface PromotionLogicMyPromotionIntentConsumeError : PromotionLogicBase <PromotionLogicMyPromotionIntent>
@property (class, readonly, getter=shared) PromotionLogicMyPromotionIntentConsumeError *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)consumeError __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MyPromotionIntentLoadInitialIfNeeded")))
@interface PromotionLogicMyPromotionIntentLoadInitialIfNeeded : PromotionLogicBase <PromotionLogicMyPromotionIntent>
@property (class, readonly, getter=shared) PromotionLogicMyPromotionIntentLoadInitialIfNeeded *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)loadInitialIfNeeded __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MyPromotionIntentLoadMore")))
@interface PromotionLogicMyPromotionIntentLoadMore : PromotionLogicBase <PromotionLogicMyPromotionIntent>
@property (class, readonly, getter=shared) PromotionLogicMyPromotionIntentLoadMore *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)loadMore __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MyPromotionIntentRefresh")))
@interface PromotionLogicMyPromotionIntentRefresh : PromotionLogicBase <PromotionLogicMyPromotionIntent>
@property (class, readonly, getter=shared) PromotionLogicMyPromotionIntentRefresh *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)refresh __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MyPromotionIntentSearch")))
@interface PromotionLogicMyPromotionIntentSearch : PromotionLogicBase <PromotionLogicMyPromotionIntent>
@property (readonly) NSString *keyword __attribute__((swift_name("keyword")));
- (instancetype)initWithKeyword:(NSString *)keyword __attribute__((swift_name("init(keyword:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicMyPromotionIntentSearch *)doCopyKeyword:(NSString *)keyword __attribute__((swift_name("doCopy(keyword:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MyPromotionIntentSelectTab")))
@interface PromotionLogicMyPromotionIntentSelectTab : PromotionLogicBase <PromotionLogicMyPromotionIntent>
@property (readonly) NSString *tabCode __attribute__((swift_name("tabCode")));
- (instancetype)initWithTabCode:(NSString *)tabCode __attribute__((swift_name("init(tabCode:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicMyPromotionIntentSelectTab *)doCopyTabCode:(NSString *)tabCode __attribute__((swift_name("doCopy(tabCode:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MyPromotionState")))
@interface PromotionLogicMyPromotionState : PromotionLogicBase
@property (readonly) NSString * _Nullable errorCode __attribute__((swift_name("errorCode")));
@property (readonly) BOOL hasLoadedInitial __attribute__((swift_name("hasLoadedInitial")));
@property (readonly) BOOL isEmpty __attribute__((swift_name("isEmpty")));
@property (readonly) BOOL isLastPage __attribute__((swift_name("isLastPage")));
@property (readonly) BOOL isLoading __attribute__((swift_name("isLoading")));
@property (readonly) BOOL isLoadingMore __attribute__((swift_name("isLoadingMore")));
@property (readonly) BOOL isRefreshing __attribute__((swift_name("isRefreshing")));
@property (readonly) BOOL isRefreshingTab __attribute__((swift_name("isRefreshingTab")));
@property (readonly) NSString *keyword __attribute__((swift_name("keyword")));
@property (readonly) int32_t page __attribute__((swift_name("page")));
@property (readonly) NSString * _Nullable selectedTabCode __attribute__((swift_name("selectedTabCode")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@property (readonly) NSArray<PromotionLogicMyPromotionTab *> *tabs __attribute__((swift_name("tabs")));
@property (readonly) NSArray<PromotionLogicMyPromotionVoucher *> *vouchers __attribute__((swift_name("vouchers")));
- (instancetype)initWithHasLoadedInitial:(BOOL)hasLoadedInitial isLoading:(BOOL)isLoading isRefreshing:(BOOL)isRefreshing isRefreshingTab:(BOOL)isRefreshingTab isLoadingMore:(BOOL)isLoadingMore isEmpty:(BOOL)isEmpty tabs:(NSArray<PromotionLogicMyPromotionTab *> *)tabs selectedTabCode:(NSString * _Nullable)selectedTabCode keyword:(NSString *)keyword page:(int32_t)page size:(int32_t)size isLastPage:(BOOL)isLastPage vouchers:(NSArray<PromotionLogicMyPromotionVoucher *> *)vouchers errorCode:(NSString * _Nullable)errorCode __attribute__((swift_name("init(hasLoadedInitial:isLoading:isRefreshing:isRefreshingTab:isLoadingMore:isEmpty:tabs:selectedTabCode:keyword:page:size:isLastPage:vouchers:errorCode:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicMyPromotionState *)doCopyHasLoadedInitial:(BOOL)hasLoadedInitial isLoading:(BOOL)isLoading isRefreshing:(BOOL)isRefreshing isRefreshingTab:(BOOL)isRefreshingTab isLoadingMore:(BOOL)isLoadingMore isEmpty:(BOOL)isEmpty tabs:(NSArray<PromotionLogicMyPromotionTab *> *)tabs selectedTabCode:(NSString * _Nullable)selectedTabCode keyword:(NSString *)keyword page:(int32_t)page size:(int32_t)size isLastPage:(BOOL)isLastPage vouchers:(NSArray<PromotionLogicMyPromotionVoucher *> *)vouchers errorCode:(NSString * _Nullable)errorCode __attribute__((swift_name("doCopy(hasLoadedInitial:isLoading:isRefreshing:isRefreshingTab:isLoadingMore:isEmpty:tabs:selectedTabCode:keyword:page:size:isLastPage:vouchers:errorCode:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MyPromotionStore")))
@interface PromotionLogicMyPromotionStore : PromotionLogicBase <PromotionLogicPRMStore>
@property (readonly) id<PromotionLogicMyPromotionIntent> consumeErrorIntent __attribute__((swift_name("consumeErrorIntent")));
@property (readonly) id<PromotionLogicKotlinx_coroutines_coreStateFlow> state __attribute__((swift_name("state")));
- (instancetype)initWithSearchCustomerVouchersUseCase:(PromotionLogicSearchCustomerVouchersUseCase *)searchCustomerVouchersUseCase __attribute__((swift_name("init(searchCustomerVouchersUseCase:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithSearchCustomerVouchersUseCase:(PromotionLogicSearchCustomerVouchersUseCase *)searchCustomerVouchersUseCase scope:(id<PromotionLogicKotlinx_coroutines_coreCoroutineScope>)scope __attribute__((swift_name("init(searchCustomerVouchersUseCase:scope:)"))) __attribute__((objc_designated_initializer));
- (void)clear __attribute__((swift_name("clear()")));
- (PromotionLogicMyPromotionState *)currentState __attribute__((swift_name("currentState()")));
- (void)dispatchIntent:(id<PromotionLogicMyPromotionIntent>)intent __attribute__((swift_name("dispatch(intent:)")));
- (NSString * _Nullable)errorOfState:(PromotionLogicMyPromotionState *)state __attribute__((swift_name("errorOf(state:)")));
- (PromotionLogicPromotionCancellable *)watchStateOnEach:(void (^)(PromotionLogicMyPromotionState *))onEach __attribute__((swift_name("watchState(onEach:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MyPromotionTab")))
@interface PromotionLogicMyPromotionTab : PromotionLogicBase
@property (readonly) NSString *code __attribute__((swift_name("code")));
@property (readonly) int32_t count __attribute__((swift_name("count")));
@property (readonly) BOOL isDefault __attribute__((swift_name("isDefault")));
@property (readonly) NSString *label __attribute__((swift_name("label")));
@property (readonly) int32_t order __attribute__((swift_name("order")));
- (instancetype)initWithCode:(NSString *)code label:(NSString *)label count:(int32_t)count order:(int32_t)order isDefault:(BOOL)isDefault __attribute__((swift_name("init(code:label:count:order:isDefault:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicMyPromotionTab *)doCopyCode:(NSString *)code label:(NSString *)label count:(int32_t)count order:(int32_t)order isDefault:(BOOL)isDefault __attribute__((swift_name("doCopy(code:label:count:order:isDefault:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MyPromotionVoucher")))
@interface PromotionLogicMyPromotionVoucher : PromotionLogicBase
@property (readonly) PromotionLogicMyPromotionAction *action __attribute__((swift_name("action")));
@property (readonly) PromotionLogicMyPromotionBadge *badge __attribute__((swift_name("badge")));
@property (readonly) PromotionLogicInt * _Nullable expiringInDays __attribute__((swift_name("expiringInDays")));
@property (readonly) BOOL isEnabled __attribute__((swift_name("isEnabled")));
@property (readonly) PromotionLogicVoucherItem *source __attribute__((swift_name("source")));
- (instancetype)initWithSource:(PromotionLogicVoucherItem *)source isEnabled:(BOOL)isEnabled expiringInDays:(PromotionLogicInt * _Nullable)expiringInDays badge:(PromotionLogicMyPromotionBadge *)badge action:(PromotionLogicMyPromotionAction *)action __attribute__((swift_name("init(source:isEnabled:expiringInDays:badge:action:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicMyPromotionVoucher *)doCopySource:(PromotionLogicVoucherItem *)source isEnabled:(BOOL)isEnabled expiringInDays:(PromotionLogicInt * _Nullable)expiringInDays badge:(PromotionLogicMyPromotionBadge *)badge action:(PromotionLogicMyPromotionAction *)action __attribute__((swift_name("doCopy(source:isEnabled:expiringInDays:badge:action:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OfferWidgetAppliedDiscount")))
@interface PromotionLogicOfferWidgetAppliedDiscount : PromotionLogicBase
@property (readonly) NSString *calculatedDiscount __attribute__((swift_name("calculatedDiscount")));
@property (readonly) NSString *eligibilityStatus __attribute__((swift_name("eligibilityStatus")));
@property (readonly) NSString * _Nullable logoUrl __attribute__((swift_name("logoUrl")));
@property (readonly) NSString *objectId __attribute__((swift_name("objectId")));
@property (readonly) NSString *objectType __attribute__((swift_name("objectType")));
@property (readonly) NSArray<NSString *> *tags __attribute__((swift_name("tags")));
@property (readonly) BOOL valid __attribute__((swift_name("valid")));
@property (readonly) NSArray<NSString *> *validationMessages __attribute__((swift_name("validationMessages")));
@property (readonly) NSString * _Nullable voucherName __attribute__((swift_name("voucherName")));
- (instancetype)initWithObjectId:(NSString *)objectId objectType:(NSString *)objectType valid:(BOOL)valid calculatedDiscount:(NSString *)calculatedDiscount eligibilityStatus:(NSString *)eligibilityStatus tags:(NSArray<NSString *> *)tags voucherName:(NSString * _Nullable)voucherName logoUrl:(NSString * _Nullable)logoUrl validationMessages:(NSArray<NSString *> *)validationMessages __attribute__((swift_name("init(objectId:objectType:valid:calculatedDiscount:eligibilityStatus:tags:voucherName:logoUrl:validationMessages:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicOfferWidgetAppliedDiscount *)doCopyObjectId:(NSString *)objectId objectType:(NSString *)objectType valid:(BOOL)valid calculatedDiscount:(NSString *)calculatedDiscount eligibilityStatus:(NSString *)eligibilityStatus tags:(NSArray<NSString *> *)tags voucherName:(NSString * _Nullable)voucherName logoUrl:(NSString * _Nullable)logoUrl validationMessages:(NSArray<NSString *> *)validationMessages __attribute__((swift_name("doCopy(objectId:objectType:valid:calculatedDiscount:eligibilityStatus:tags:voucherName:logoUrl:validationMessages:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((swift_name("OfferWidgetApplyOutcome")))
@protocol PromotionLogicOfferWidgetApplyOutcome
@required
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OfferWidgetApplyOutcomeApplied")))
@interface PromotionLogicOfferWidgetApplyOutcomeApplied : PromotionLogicBase <PromotionLogicOfferWidgetApplyOutcome>
@property (class, readonly, getter=shared) PromotionLogicOfferWidgetApplyOutcomeApplied *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)applied __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OfferWidgetApplyOutcomeFailed")))
@interface PromotionLogicOfferWidgetApplyOutcomeFailed : PromotionLogicBase <PromotionLogicOfferWidgetApplyOutcome>
@property (readonly) NSString *errorCode __attribute__((swift_name("errorCode")));
- (instancetype)initWithErrorCode:(NSString *)errorCode __attribute__((swift_name("init(errorCode:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicOfferWidgetApplyOutcomeFailed *)doCopyErrorCode:(NSString *)errorCode __attribute__((swift_name("doCopy(errorCode:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OfferWidgetApplyOutcomeRejected")))
@interface PromotionLogicOfferWidgetApplyOutcomeRejected : PromotionLogicBase <PromotionLogicOfferWidgetApplyOutcome>
@property (readonly) NSArray<PromotionLogicRejectedOffer *> *items __attribute__((swift_name("items")));
- (instancetype)initWithItems:(NSArray<PromotionLogicRejectedOffer *> *)items __attribute__((swift_name("init(items:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicOfferWidgetApplyOutcomeRejected *)doCopyItems:(NSArray<PromotionLogicRejectedOffer *> *)items __attribute__((swift_name("doCopy(items:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((swift_name("OfferWidgetConfirmResult")))
@protocol PromotionLogicOfferWidgetConfirmResult
@required
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OfferWidgetConfirmResultFailure")))
@interface PromotionLogicOfferWidgetConfirmResultFailure : PromotionLogicBase <PromotionLogicOfferWidgetConfirmResult>
@property (readonly) NSString *errorCode __attribute__((swift_name("errorCode")));
- (instancetype)initWithErrorCode:(NSString *)errorCode __attribute__((swift_name("init(errorCode:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicOfferWidgetConfirmResultFailure *)doCopyErrorCode:(NSString *)errorCode __attribute__((swift_name("doCopy(errorCode:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OfferWidgetConfirmResultSuccess")))
@interface PromotionLogicOfferWidgetConfirmResultSuccess : PromotionLogicBase <PromotionLogicOfferWidgetConfirmResult>
@property (class, readonly, getter=shared) PromotionLogicOfferWidgetConfirmResultSuccess *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)success __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OfferWidgetDisplayState")))
@interface PromotionLogicOfferWidgetDisplayState : PromotionLogicKotlinEnum<PromotionLogicOfferWidgetDisplayState *>
@property (class, readonly) PromotionLogicOfferWidgetDisplayState *empty __attribute__((swift_name("empty")));
@property (class, readonly) PromotionLogicOfferWidgetDisplayState *notApplied __attribute__((swift_name("notApplied")));
@property (class, readonly) PromotionLogicOfferWidgetDisplayState *applied __attribute__((swift_name("applied")));
@property (class, readonly) PromotionLogicOfferWidgetDisplayState *unavailable __attribute__((swift_name("unavailable")));
@property (class, readonly) NSArray<PromotionLogicOfferWidgetDisplayState *> *entries __attribute__((swift_name("entries")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (PromotionLogicKotlinArray<PromotionLogicOfferWidgetDisplayState *> *)values __attribute__((swift_name("values()")));
@end
__attribute__((swift_name("OfferWidgetHostEvent")))
@protocol PromotionLogicOfferWidgetHostEvent
@required
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OfferWidgetHostEventVoucherApplied")))
@interface PromotionLogicOfferWidgetHostEventVoucherApplied : PromotionLogicBase <PromotionLogicOfferWidgetHostEvent>
@property (readonly) NSString *voucherId __attribute__((swift_name("voucherId")));
- (instancetype)initWithVoucherId:(NSString *)voucherId __attribute__((swift_name("init(voucherId:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicOfferWidgetHostEventVoucherApplied *)doCopyVoucherId:(NSString *)voucherId __attribute__((swift_name("doCopy(voucherId:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OfferWidgetHostNotifier")))
@interface PromotionLogicOfferWidgetHostNotifier : PromotionLogicBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSArray<id<PromotionLogicOfferWidgetHostEvent>> *)onStateState:(PromotionLogicOfferWidgetState *)state __attribute__((swift_name("onState(state:)")));
@end
__attribute__((swift_name("OfferWidgetIntent")))
@protocol PromotionLogicOfferWidgetIntent
@required
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OfferWidgetIntentClearApplied")))
@interface PromotionLogicOfferWidgetIntentClearApplied : PromotionLogicBase <PromotionLogicOfferWidgetIntent>
@property (class, readonly, getter=shared) PromotionLogicOfferWidgetIntentClearApplied *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)clearApplied __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OfferWidgetIntentConsumeError")))
@interface PromotionLogicOfferWidgetIntentConsumeError : PromotionLogicBase <PromotionLogicOfferWidgetIntent>
@property (class, readonly, getter=shared) PromotionLogicOfferWidgetIntentConsumeError *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)consumeError __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OfferWidgetIntentLoadInitial")))
@interface PromotionLogicOfferWidgetIntentLoadInitial : PromotionLogicBase <PromotionLogicOfferWidgetIntent>
@property (class, readonly, getter=shared) PromotionLogicOfferWidgetIntentLoadInitial *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)loadInitial __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OfferWidgetIntentMarkUnavailable")))
@interface PromotionLogicOfferWidgetIntentMarkUnavailable : PromotionLogicBase <PromotionLogicOfferWidgetIntent>
@property (class, readonly, getter=shared) PromotionLogicOfferWidgetIntentMarkUnavailable *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)markUnavailable __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OfferWidgetIntentSetApplied")))
@interface PromotionLogicOfferWidgetIntentSetApplied : PromotionLogicBase <PromotionLogicOfferWidgetIntent>
@property (readonly) NSArray<PromotionLogicOfferWidgetAppliedDiscount *> *discounts __attribute__((swift_name("discounts")));
@property (readonly) BOOL unavailable __attribute__((swift_name("unavailable")));
- (instancetype)initWithDiscounts:(NSArray<PromotionLogicOfferWidgetAppliedDiscount *> *)discounts unavailable:(BOOL)unavailable __attribute__((swift_name("init(discounts:unavailable:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicOfferWidgetIntentSetApplied *)doCopyDiscounts:(NSArray<PromotionLogicOfferWidgetAppliedDiscount *> *)discounts unavailable:(BOOL)unavailable __attribute__((swift_name("doCopy(discounts:unavailable:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OfferWidgetIntentValidateAndApply")))
@interface PromotionLogicOfferWidgetIntentValidateAndApply : PromotionLogicBase <PromotionLogicOfferWidgetIntent>
@property (readonly) NSArray<PromotionLogicEligibleOffer *> *offers __attribute__((swift_name("offers")));
- (instancetype)initWithOffers:(NSArray<PromotionLogicEligibleOffer *> *)offers __attribute__((swift_name("init(offers:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicOfferWidgetIntentValidateAndApply *)doCopyOffers:(NSArray<PromotionLogicEligibleOffer *> *)offers __attribute__((swift_name("doCopy(offers:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OfferWidgetState")))
@interface PromotionLogicOfferWidgetState : PromotionLogicBase
@property (readonly) NSArray<PromotionLogicOfferWidgetAppliedDiscount *> *appliedDiscounts __attribute__((swift_name("appliedDiscounts")));
@property (readonly) BOOL discountUnavailable __attribute__((swift_name("discountUnavailable")));
@property (readonly) NSString * _Nullable errorCode __attribute__((swift_name("errorCode")));
@property (readonly) BOOL hasLoadedInitial __attribute__((swift_name("hasLoadedInitial")));
@property (readonly) BOOL isLoading __attribute__((swift_name("isLoading")));
@property (readonly) BOOL isValidating __attribute__((swift_name("isValidating")));
@property (readonly) BOOL myIsLastPage __attribute__((swift_name("myIsLastPage")));
@property (readonly) NSArray<PromotionLogicEligibleOffer *> *myOffers __attribute__((swift_name("myOffers")));
@property (readonly) BOOL otherIsLastPage __attribute__((swift_name("otherIsLastPage")));
@property (readonly) NSArray<PromotionLogicEligibleOffer *> *otherOffers __attribute__((swift_name("otherOffers")));
@property (readonly) int32_t totalVoucherCount __attribute__((swift_name("totalVoucherCount")));
@property (readonly) PromotionLogicOfferWidgetDisplayState *widgetState __attribute__((swift_name("widgetState")));
- (instancetype)initWithHasLoadedInitial:(BOOL)hasLoadedInitial isLoading:(BOOL)isLoading myOffers:(NSArray<PromotionLogicEligibleOffer *> *)myOffers otherOffers:(NSArray<PromotionLogicEligibleOffer *> *)otherOffers myIsLastPage:(BOOL)myIsLastPage otherIsLastPage:(BOOL)otherIsLastPage totalVoucherCount:(int32_t)totalVoucherCount appliedDiscounts:(NSArray<PromotionLogicOfferWidgetAppliedDiscount *> *)appliedDiscounts discountUnavailable:(BOOL)discountUnavailable isValidating:(BOOL)isValidating errorCode:(NSString * _Nullable)errorCode __attribute__((swift_name("init(hasLoadedInitial:isLoading:myOffers:otherOffers:myIsLastPage:otherIsLastPage:totalVoucherCount:appliedDiscounts:discountUnavailable:isValidating:errorCode:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicOfferWidgetState *)doCopyHasLoadedInitial:(BOOL)hasLoadedInitial isLoading:(BOOL)isLoading myOffers:(NSArray<PromotionLogicEligibleOffer *> *)myOffers otherOffers:(NSArray<PromotionLogicEligibleOffer *> *)otherOffers myIsLastPage:(BOOL)myIsLastPage otherIsLastPage:(BOOL)otherIsLastPage totalVoucherCount:(int32_t)totalVoucherCount appliedDiscounts:(NSArray<PromotionLogicOfferWidgetAppliedDiscount *> *)appliedDiscounts discountUnavailable:(BOOL)discountUnavailable isValidating:(BOOL)isValidating errorCode:(NSString * _Nullable)errorCode __attribute__((swift_name("doCopy(hasLoadedInitial:isLoading:myOffers:otherOffers:myIsLastPage:otherIsLastPage:totalVoucherCount:appliedDiscounts:discountUnavailable:isValidating:errorCode:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OfferWidgetStore")))
@interface PromotionLogicOfferWidgetStore : PromotionLogicBase <PromotionLogicPRMStore>
@property (readonly) id<PromotionLogicOfferWidgetIntent> consumeErrorIntent __attribute__((swift_name("consumeErrorIntent")));
@property (readonly) id<PromotionLogicKotlinx_coroutines_coreStateFlow> state __attribute__((swift_name("state")));
- (instancetype)initWithFindEligibleCampaignsUseCase:(PromotionLogicFindEligibleCampaignsUseCase *)findEligibleCampaignsUseCase validateStackableDiscountsUseCase:(PromotionLogicValidateStackableDiscountsUseCase *)validateStackableDiscountsUseCase createRedemptionSessionUseCase:(PromotionLogicCreateRedemptionSessionUseCase *)createRedemptionSessionUseCase __attribute__((swift_name("init(findEligibleCampaignsUseCase:validateStackableDiscountsUseCase:createRedemptionSessionUseCase:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithFindEligibleCampaignsUseCase:(PromotionLogicFindEligibleCampaignsUseCase *)findEligibleCampaignsUseCase validateStackableDiscountsUseCase:(PromotionLogicValidateStackableDiscountsUseCase *)validateStackableDiscountsUseCase createRedemptionSessionUseCase:(PromotionLogicCreateRedemptionSessionUseCase *)createRedemptionSessionUseCase scope:(id<PromotionLogicKotlinx_coroutines_coreCoroutineScope>)scope __attribute__((swift_name("init(findEligibleCampaignsUseCase:validateStackableDiscountsUseCase:createRedemptionSessionUseCase:scope:)"))) __attribute__((objc_designated_initializer));
- (BOOL)availabilityFromCache __attribute__((swift_name("availabilityFromCache()")));
- (void)clear __attribute__((swift_name("clear()")));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)confirmRedemptionWithCompletionHandler:(void (^)(id<PromotionLogicOfferWidgetConfirmResult> _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("confirmRedemption(completionHandler:)")));
- (PromotionLogicOfferWidgetState *)currentState __attribute__((swift_name("currentState()")));
- (void)dispatchIntent:(id<PromotionLogicOfferWidgetIntent>)intent __attribute__((swift_name("dispatch(intent:)")));
- (NSString * _Nullable)errorOfState:(PromotionLogicOfferWidgetState *)state __attribute__((swift_name("errorOf(state:)")));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)refreshAvailabilityWithCompletionHandler:(void (^)(PromotionLogicBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("refreshAvailability(completionHandler:)")));
/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)validateAndApplyOffers:(NSArray<PromotionLogicEligibleOffer *> *)offers completionHandler:(void (^)(id<PromotionLogicOfferWidgetApplyOutcome> _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("validateAndApply(offers:completionHandler:)")));
- (PromotionLogicPromotionCancellable *)watchStateOnEach:(void (^)(PromotionLogicOfferWidgetState *))onEach __attribute__((swift_name("watchState(onEach:)")));
@end
__attribute__((swift_name("PromotionDetailIntent")))
@protocol PromotionLogicPromotionDetailIntent
@required
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PromotionDetailIntentConsumeError")))
@interface PromotionLogicPromotionDetailIntentConsumeError : PromotionLogicBase <PromotionLogicPromotionDetailIntent>
@property (class, readonly, getter=shared) PromotionLogicPromotionDetailIntentConsumeError *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)consumeError __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PromotionDetailIntentLoadDetail")))
@interface PromotionLogicPromotionDetailIntentLoadDetail : PromotionLogicBase <PromotionLogicPromotionDetailIntent>
@property (readonly) NSString *voucherId __attribute__((swift_name("voucherId")));
- (instancetype)initWithVoucherId:(NSString *)voucherId __attribute__((swift_name("init(voucherId:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicPromotionDetailIntentLoadDetail *)doCopyVoucherId:(NSString *)voucherId __attribute__((swift_name("doCopy(voucherId:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PromotionDetailState")))
@interface PromotionLogicPromotionDetailState : PromotionLogicBase
@property (readonly) BOOL actionEnabled __attribute__((swift_name("actionEnabled")));
@property (readonly) NSString *actionLabel __attribute__((swift_name("actionLabel")));
@property (readonly) BOOL actionVisible __attribute__((swift_name("actionVisible")));
@property (readonly) PromotionLogicVoucherDetail * _Nullable detail __attribute__((swift_name("detail")));
@property (readonly) NSString * _Nullable errorCode __attribute__((swift_name("errorCode")));
@property (readonly) BOOL isLoading __attribute__((swift_name("isLoading")));
@property (readonly) PromotionLogicVoucherStatus *status __attribute__((swift_name("status")));
- (instancetype)initWithIsLoading:(BOOL)isLoading detail:(PromotionLogicVoucherDetail * _Nullable)detail status:(PromotionLogicVoucherStatus *)status actionVisible:(BOOL)actionVisible actionEnabled:(BOOL)actionEnabled actionLabel:(NSString *)actionLabel errorCode:(NSString * _Nullable)errorCode __attribute__((swift_name("init(isLoading:detail:status:actionVisible:actionEnabled:actionLabel:errorCode:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicPromotionDetailState *)doCopyIsLoading:(BOOL)isLoading detail:(PromotionLogicVoucherDetail * _Nullable)detail status:(PromotionLogicVoucherStatus *)status actionVisible:(BOOL)actionVisible actionEnabled:(BOOL)actionEnabled actionLabel:(NSString *)actionLabel errorCode:(NSString * _Nullable)errorCode __attribute__((swift_name("doCopy(isLoading:detail:status:actionVisible:actionEnabled:actionLabel:errorCode:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PromotionDetailStore")))
@interface PromotionLogicPromotionDetailStore : PromotionLogicBase <PromotionLogicPRMStore>
@property (readonly) id<PromotionLogicPromotionDetailIntent> consumeErrorIntent __attribute__((swift_name("consumeErrorIntent")));
@property (readonly) id<PromotionLogicKotlinx_coroutines_coreStateFlow> state __attribute__((swift_name("state")));
- (instancetype)initWithGetCustomerVoucherDetailUseCase:(PromotionLogicGetCustomerVoucherDetailUseCase *)getCustomerVoucherDetailUseCase __attribute__((swift_name("init(getCustomerVoucherDetailUseCase:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithGetCustomerVoucherDetailUseCase:(PromotionLogicGetCustomerVoucherDetailUseCase *)getCustomerVoucherDetailUseCase scope:(id<PromotionLogicKotlinx_coroutines_coreCoroutineScope>)scope __attribute__((swift_name("init(getCustomerVoucherDetailUseCase:scope:)"))) __attribute__((objc_designated_initializer));
- (void)clear __attribute__((swift_name("clear()")));
- (PromotionLogicPromotionDetailState *)currentState __attribute__((swift_name("currentState()")));
- (void)dispatchIntent:(id<PromotionLogicPromotionDetailIntent>)intent __attribute__((swift_name("dispatch(intent:)")));
- (NSString * _Nullable)errorOfState:(PromotionLogicPromotionDetailState *)state __attribute__((swift_name("errorOf(state:)")));
- (PromotionLogicPromotionCancellable *)watchStateOnEach:(void (^)(PromotionLogicPromotionDetailState *))onEach __attribute__((swift_name("watchState(onEach:)")));
@end
__attribute__((swift_name("SearchMyPromotionIntent")))
@protocol PromotionLogicSearchMyPromotionIntent
@required
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SearchMyPromotionIntentClearKeyword")))
@interface PromotionLogicSearchMyPromotionIntentClearKeyword : PromotionLogicBase <PromotionLogicSearchMyPromotionIntent>
@property (class, readonly, getter=shared) PromotionLogicSearchMyPromotionIntentClearKeyword *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)clearKeyword __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SearchMyPromotionIntentConsumeError")))
@interface PromotionLogicSearchMyPromotionIntentConsumeError : PromotionLogicBase <PromotionLogicSearchMyPromotionIntent>
@property (class, readonly, getter=shared) PromotionLogicSearchMyPromotionIntentConsumeError *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)consumeError __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SearchMyPromotionIntentLoadMore")))
@interface PromotionLogicSearchMyPromotionIntentLoadMore : PromotionLogicBase <PromotionLogicSearchMyPromotionIntent>
@property (class, readonly, getter=shared) PromotionLogicSearchMyPromotionIntentLoadMore *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)loadMore __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SearchMyPromotionIntentQueryChanged")))
@interface PromotionLogicSearchMyPromotionIntentQueryChanged : PromotionLogicBase <PromotionLogicSearchMyPromotionIntent>
@property (readonly) NSString *keyword __attribute__((swift_name("keyword")));
- (instancetype)initWithKeyword:(NSString *)keyword __attribute__((swift_name("init(keyword:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicSearchMyPromotionIntentQueryChanged *)doCopyKeyword:(NSString *)keyword __attribute__((swift_name("doCopy(keyword:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SearchMyPromotionIntentRetry")))
@interface PromotionLogicSearchMyPromotionIntentRetry : PromotionLogicBase <PromotionLogicSearchMyPromotionIntent>
@property (class, readonly, getter=shared) PromotionLogicSearchMyPromotionIntentRetry *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)retry __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SearchMyPromotionIntentSearch")))
@interface PromotionLogicSearchMyPromotionIntentSearch : PromotionLogicBase <PromotionLogicSearchMyPromotionIntent>
@property (class, readonly, getter=shared) PromotionLogicSearchMyPromotionIntentSearch *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)search __attribute__((swift_name("init()")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SearchMyPromotionState")))
@interface PromotionLogicSearchMyPromotionState : PromotionLogicBase
@property (readonly) NSString * _Nullable errorCode __attribute__((swift_name("errorCode")));
@property (readonly) BOOL isEmpty __attribute__((swift_name("isEmpty")));
@property (readonly) BOOL isLastPage __attribute__((swift_name("isLastPage")));
@property (readonly) BOOL isLoading __attribute__((swift_name("isLoading")));
@property (readonly) BOOL isLoadingMore __attribute__((swift_name("isLoadingMore")));
@property (readonly) NSString *keyword __attribute__((swift_name("keyword")));
@property (readonly) int32_t page __attribute__((swift_name("page")));
@property (readonly) int32_t pageSize __attribute__((swift_name("pageSize")));
@property (readonly) NSArray<PromotionLogicMyPromotionVoucher *> *vouchers __attribute__((swift_name("vouchers")));
- (instancetype)initWithKeyword:(NSString *)keyword isLoading:(BOOL)isLoading isLoadingMore:(BOOL)isLoadingMore isEmpty:(BOOL)isEmpty isLastPage:(BOOL)isLastPage page:(int32_t)page pageSize:(int32_t)pageSize vouchers:(NSArray<PromotionLogicMyPromotionVoucher *> *)vouchers errorCode:(NSString * _Nullable)errorCode __attribute__((swift_name("init(keyword:isLoading:isLoadingMore:isEmpty:isLastPage:page:pageSize:vouchers:errorCode:)"))) __attribute__((objc_designated_initializer));
- (PromotionLogicSearchMyPromotionState *)doCopyKeyword:(NSString *)keyword isLoading:(BOOL)isLoading isLoadingMore:(BOOL)isLoadingMore isEmpty:(BOOL)isEmpty isLastPage:(BOOL)isLastPage page:(int32_t)page pageSize:(int32_t)pageSize vouchers:(NSArray<PromotionLogicMyPromotionVoucher *> *)vouchers errorCode:(NSString * _Nullable)errorCode __attribute__((swift_name("doCopy(keyword:isLoading:isLoadingMore:isEmpty:isLastPage:page:pageSize:vouchers:errorCode:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SearchMyPromotionStore")))
@interface PromotionLogicSearchMyPromotionStore : PromotionLogicBase <PromotionLogicPRMStore>
@property (readonly) id<PromotionLogicSearchMyPromotionIntent> consumeErrorIntent __attribute__((swift_name("consumeErrorIntent")));
@property (readonly) id<PromotionLogicKotlinx_coroutines_coreStateFlow> state __attribute__((swift_name("state")));
- (instancetype)initWithSearchCustomerVouchersUseCase:(PromotionLogicSearchCustomerVouchersUseCase *)searchCustomerVouchersUseCase __attribute__((swift_name("init(searchCustomerVouchersUseCase:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithSearchCustomerVouchersUseCase:(PromotionLogicSearchCustomerVouchersUseCase *)searchCustomerVouchersUseCase scope:(id<PromotionLogicKotlinx_coroutines_coreCoroutineScope>)scope __attribute__((swift_name("init(searchCustomerVouchersUseCase:scope:)"))) __attribute__((objc_designated_initializer));
- (void)clear __attribute__((swift_name("clear()")));
- (PromotionLogicSearchMyPromotionState *)currentState __attribute__((swift_name("currentState()")));
- (void)dispatchIntent:(id<PromotionLogicSearchMyPromotionIntent>)intent __attribute__((swift_name("dispatch(intent:)")));
- (NSString * _Nullable)errorOfState:(PromotionLogicSearchMyPromotionState *)state __attribute__((swift_name("errorOf(state:)")));
- (PromotionLogicPromotionCancellable *)watchStateOnEach:(void (^)(PromotionLogicSearchMyPromotionState *))onEach __attribute__((swift_name("watchState(onEach:)")));
@end
@interface PromotionLogicVoucherDetail (Extensions)
- (PromotionLogicVoucherDisplayState *)displayState __attribute__((swift_name("displayState()")));
@end
@interface PromotionLogicVoucherItem (Extensions)
- (PromotionLogicVoucherDisplayState *)displayState __attribute__((swift_name("displayState()")));
@end
@interface PromotionLogicChooseOffer (Extensions)
- (BOOL)showsIneligibleWarning __attribute__((swift_name("showsIneligibleWarning()")));
@end
@interface PromotionLogicChoosePromotionState (Extensions)
- (NSArray<PromotionLogicEligibleOffer *> *)allOffers __attribute__((swift_name("allOffers()")));
- (BOOL)canApply __attribute__((swift_name("canApply()")));
- (NSString *)highlightKeyword __attribute__((swift_name("highlightKeyword()")));
- (BOOL)isSelectedId:(NSString *)id __attribute__((swift_name("isSelected(id:)")));
- (PromotionLogicChooseSeeMoreState *)mySeeMoreState __attribute__((swift_name("mySeeMoreState()")));
- (NSArray<PromotionLogicEligibleOffer *> *)selectedOffers __attribute__((swift_name("selectedOffers()")));
- (BOOL)shouldLoadMoreOtherVisibleIndex:(int32_t)visibleIndex __attribute__((swift_name("shouldLoadMoreOther(visibleIndex:)")));
- (BOOL)showsEmptyView __attribute__((swift_name("showsEmptyView()")));
- (BOOL)showsNoResult __attribute__((swift_name("showsNoResult()")));
- (BOOL)showsSelectedCount __attribute__((swift_name("showsSelectedCount()")));
- (NSArray<PromotionLogicChooseOffer *> *)visibleMyOffers __attribute__((swift_name("visibleMyOffers()")));
@end
@interface PromotionLogicMyPromotionState (Extensions)
- (PromotionLogicMyPromotionVoucher * _Nullable)voucherId:(NSString *)id __attribute__((swift_name("voucher(id:)")));
@end
@interface PromotionLogicSearchMyPromotionState (Extensions)
- (BOOL)showsNoResult __attribute__((swift_name("showsNoResult()")));
- (BOOL)showsResults __attribute__((swift_name("showsResults()")));
- (PromotionLogicMyPromotionVoucher * _Nullable)voucherId:(NSString *)id __attribute__((swift_name("voucher(id:)")));
@end
@interface PromotionLogicKotlinThrowable (Extensions)
- (NSString *)toErrorCode __attribute__((swift_name("toErrorCode()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChoosePromotionContractKt")))
@interface PromotionLogicChoosePromotionContractKt : PromotionLogicBase
@property (class, readonly) int32_t COLLAPSED_MY_COUNT __attribute__((swift_name("COLLAPSED_MY_COUNT")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PromotionHtmlContentKt")))
@interface PromotionLogicPromotionHtmlContentKt : PromotionLogicBase
+ (NSString *)wrapPromotionHtmlContent:(NSString *)content __attribute__((swift_name("wrapPromotionHtml(content:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SearchConfigKt")))
@interface PromotionLogicSearchConfigKt : PromotionLogicBase
@property (class, readonly) int32_t PROMOTION_SEARCH_MAX_LENGTH __attribute__((swift_name("PROMOTION_SEARCH_MAX_LENGTH")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ServiceSelectorKt")))
@interface PromotionLogicServiceSelectorKt : PromotionLogicBase
+ (NSArray<PromotionLogicAvailableService *> *)configuredServicesForApplicableProducts:(NSArray<PromotionLogicApplicableProduct *> *)applicableProducts __attribute__((swift_name("configuredServicesFor(applicableProducts:)")));
+ (NSArray<PromotionLogicAvailableService *> *)servicesForApplicableProductsApplicableProducts:(NSArray<PromotionLogicApplicableProduct *> *)applicableProducts availableServices:(NSArray<PromotionLogicAvailableService *> *)availableServices __attribute__((swift_name("servicesForApplicableProducts(applicableProducts:availableServices:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("__SkieSuspendWrappersKt")))
@interface PromotionLogic__SkieSuspendWrappersKt : PromotionLogicBase
+ (void)Skie_Suspend__0__hasNextDispatchReceiver:(PromotionLogicSkieColdFlowIterator<id> *)dispatchReceiver suspendHandler:(PromotionLogicSkie_SuspendHandler *)suspendHandler __attribute__((swift_name("Skie_Suspend__0__hasNext(dispatchReceiver:suspendHandler:)")));
+ (void)Skie_Suspend__10__findEligibleDispatchReceiver:(PromotionLogicPromotionUseCases *)dispatchReceiver request:(PromotionLogicFindEligibleCampaignsRequest *)request suspendHandler:(PromotionLogicSkie_SuspendHandler *)suspendHandler __attribute__((swift_name("Skie_Suspend__10__findEligible(dispatchReceiver:request:suspendHandler:)")));
+ (void)Skie_Suspend__11__getVoucherDetailDispatchReceiver:(PromotionLogicPromotionUseCases *)dispatchReceiver voucherId:(NSString *)voucherId service:(NSString * _Nullable)service suspendHandler:(PromotionLogicSkie_SuspendHandler *)suspendHandler __attribute__((swift_name("Skie_Suspend__11__getVoucherDetail(dispatchReceiver:voucherId:service:suspendHandler:)")));
+ (void)Skie_Suspend__12__searchVouchersDispatchReceiver:(PromotionLogicPromotionUseCases *)dispatchReceiver request:(PromotionLogicSearchCustomerVouchersRequest *)request suspendHandler:(PromotionLogicSkie_SuspendHandler *)suspendHandler __attribute__((swift_name("Skie_Suspend__12__searchVouchers(dispatchReceiver:request:suspendHandler:)")));
+ (void)Skie_Suspend__13__validateDiscountsDispatchReceiver:(PromotionLogicPromotionUseCases *)dispatchReceiver request:(PromotionLogicValidateDiscountsRequest *)request suspendHandler:(PromotionLogicSkie_SuspendHandler *)suspendHandler __attribute__((swift_name("Skie_Suspend__13__validateDiscounts(dispatchReceiver:request:suspendHandler:)")));
+ (void)Skie_Suspend__14__invokeDispatchReceiver:(PromotionLogicSearchCustomerVouchersUseCase *)dispatchReceiver request:(PromotionLogicSearchCustomerVouchersRequest *)request suspendHandler:(PromotionLogicSkie_SuspendHandler *)suspendHandler __attribute__((swift_name("Skie_Suspend__14__invoke(dispatchReceiver:request:suspendHandler:)")));
+ (void)Skie_Suspend__15__invokeDispatchReceiver:(PromotionLogicValidateStackableDiscountsUseCase *)dispatchReceiver request:(PromotionLogicValidateDiscountsRequest *)request suspendHandler:(PromotionLogicSkie_SuspendHandler *)suspendHandler __attribute__((swift_name("Skie_Suspend__15__invoke(dispatchReceiver:request:suspendHandler:)")));
+ (void)Skie_Suspend__16__confirmRedemptionDispatchReceiver:(PromotionLogicOfferWidgetStore *)dispatchReceiver suspendHandler:(PromotionLogicSkie_SuspendHandler *)suspendHandler __attribute__((swift_name("Skie_Suspend__16__confirmRedemption(dispatchReceiver:suspendHandler:)")));
+ (void)Skie_Suspend__17__refreshAvailabilityDispatchReceiver:(PromotionLogicOfferWidgetStore *)dispatchReceiver suspendHandler:(PromotionLogicSkie_SuspendHandler *)suspendHandler __attribute__((swift_name("Skie_Suspend__17__refreshAvailability(dispatchReceiver:suspendHandler:)")));
+ (void)Skie_Suspend__18__validateAndApplyDispatchReceiver:(PromotionLogicOfferWidgetStore *)dispatchReceiver offers:(NSArray<PromotionLogicEligibleOffer *> *)offers suspendHandler:(PromotionLogicSkie_SuspendHandler *)suspendHandler __attribute__((swift_name("Skie_Suspend__18__validateAndApply(dispatchReceiver:offers:suspendHandler:)")));
+ (void)Skie_Suspend__1__collectDispatchReceiver:(id<PromotionLogicKotlinx_coroutines_coreFlow>)dispatchReceiver collector:(id<PromotionLogicKotlinx_coroutines_coreFlowCollector>)collector suspendHandler:(PromotionLogicSkie_SuspendHandler *)suspendHandler __attribute__((swift_name("Skie_Suspend__1__collect(dispatchReceiver:collector:suspendHandler:)")));
+ (void)Skie_Suspend__2__emitDispatchReceiver:(id<PromotionLogicKotlinx_coroutines_coreFlowCollector>)dispatchReceiver value:(id _Nullable)value suspendHandler:(PromotionLogicSkie_SuspendHandler *)suspendHandler __attribute__((swift_name("Skie_Suspend__2__emit(dispatchReceiver:value:suspendHandler:)")));
+ (void)Skie_Suspend__3__invokeDispatchReceiver:(PromotionLogicCreateRedemptionSessionUseCase *)dispatchReceiver request:(PromotionLogicCreateRedemptionRequest *)request suspendHandler:(PromotionLogicSkie_SuspendHandler *)suspendHandler __attribute__((swift_name("Skie_Suspend__3__invoke(dispatchReceiver:request:suspendHandler:)")));
+ (void)Skie_Suspend__4__invokeDispatchReceiver:(PromotionLogicFetchFeatureFlagsUseCase *)dispatchReceiver suspendHandler:(PromotionLogicSkie_SuspendHandler *)suspendHandler __attribute__((swift_name("Skie_Suspend__4__invoke(dispatchReceiver:suspendHandler:)")));
+ (void)Skie_Suspend__5__refreshDispatchReceiver:(PromotionLogicPromotionFeatureFlagUseCases *)dispatchReceiver suspendHandler:(PromotionLogicSkie_SuspendHandler *)suspendHandler __attribute__((swift_name("Skie_Suspend__5__refresh(dispatchReceiver:suspendHandler:)")));
+ (void)Skie_Suspend__6__invokeDispatchReceiver:(PromotionLogicFindEligibleCampaignsUseCase *)dispatchReceiver request:(PromotionLogicFindEligibleCampaignsRequest *)request suspendHandler:(PromotionLogicSkie_SuspendHandler *)suspendHandler __attribute__((swift_name("Skie_Suspend__6__invoke(dispatchReceiver:request:suspendHandler:)")));
+ (void)Skie_Suspend__7__invokeDispatchReceiver:(PromotionLogicGetCustomerVoucherDetailUseCase *)dispatchReceiver voucherId:(NSString *)voucherId service:(NSString * _Nullable)service suspendHandler:(PromotionLogicSkie_SuspendHandler *)suspendHandler __attribute__((swift_name("Skie_Suspend__7__invoke(dispatchReceiver:voucherId:service:suspendHandler:)")));
+ (void)Skie_Suspend__8__refreshDispatchReceiver:(PromotionLogicPromotionFeatureGate *)dispatchReceiver suspendHandler:(PromotionLogicSkie_SuspendHandler *)suspendHandler __attribute__((swift_name("Skie_Suspend__8__refresh(dispatchReceiver:suspendHandler:)")));
+ (void)Skie_Suspend__9__createRedemptionDispatchReceiver:(PromotionLogicPromotionUseCases *)dispatchReceiver request:(PromotionLogicCreateRedemptionRequest *)request suspendHandler:(PromotionLogicSkie_SuspendHandler *)suspendHandler __attribute__((swift_name("Skie_Suspend__9__createRedemption(dispatchReceiver:request:suspendHandler:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("__SkieTypeExportsKt")))
@interface PromotionLogic__SkieTypeExportsKt : PromotionLogicBase
+ (void)skieTypeExports_0P0:(PromotionLogicKotlinx_serialization_corePolymorphicKind *)p0 p1:(PromotionLogicKotlinx_serialization_corePolymorphicKindOPEN *)p1 p2:(PromotionLogicKotlinx_serialization_corePolymorphicKindSEALED *)p2 p3:(PromotionLogicKotlinx_serialization_corePrimitiveKind *)p3 p4:(PromotionLogicKotlinx_serialization_corePrimitiveKindBOOLEAN *)p4 p5:(PromotionLogicKotlinx_serialization_corePrimitiveKindBYTE *)p5 p6:(PromotionLogicKotlinx_serialization_corePrimitiveKindCHAR *)p6 p7:(PromotionLogicKotlinx_serialization_corePrimitiveKindDOUBLE *)p7 p8:(PromotionLogicKotlinx_serialization_corePrimitiveKindFLOAT *)p8 p9:(PromotionLogicKotlinx_serialization_corePrimitiveKindINT *)p9 p10:(PromotionLogicKotlinx_serialization_corePrimitiveKindLONG *)p10 p11:(PromotionLogicKotlinx_serialization_corePrimitiveKindSHORT *)p11 p12:(PromotionLogicKotlinx_serialization_corePrimitiveKindSTRING *)p12 p13:(PromotionLogicKotlinx_serialization_coreSerialKindCONTEXTUAL *)p13 p14:(PromotionLogicKotlinx_serialization_coreSerialKindENUM *)p14 p15:(PromotionLogicKotlinx_serialization_coreStructureKind *)p15 p16:(PromotionLogicKotlinx_serialization_coreStructureKindCLASS *)p16 p17:(PromotionLogicKotlinx_serialization_coreStructureKindLIST *)p17 p18:(PromotionLogicKotlinx_serialization_coreStructureKindMAP *)p18 p19:(PromotionLogicKotlinx_serialization_coreStructureKindOBJECT *)p19 p20:(PromotionLogicKotlinx_serialization_jsonJsonNull *)p20 p21:(PromotionLogicKotlinx_serialization_jsonJsonPrimitive *)p21 __attribute__((swift_name("skieTypeExports_0(p0:p1:p2:p3:p4:p5:p6:p7:p8:p9:p10:p11:p12:p13:p14:p15:p16:p17:p18:p19:p20:p21:)")));
@end
__attribute__((swift_name("KotlinIllegalStateException")))
@interface PromotionLogicKotlinIllegalStateException : PromotionLogicKotlinRuntimeException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(PromotionLogicKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(PromotionLogicKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end
/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
__attribute__((swift_name("KotlinCancellationException")))
@interface PromotionLogicKotlinCancellationException : PromotionLogicKotlinIllegalStateException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(PromotionLogicKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(PromotionLogicKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end
__attribute__((swift_name("Kotlinx_coroutines_coreRunnable")))
@protocol PromotionLogicKotlinx_coroutines_coreRunnable
@required
- (void)run __attribute__((swift_name("run()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinEnumCompanion")))
@interface PromotionLogicKotlinEnumCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicKotlinEnumCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinArray")))
@interface PromotionLogicKotlinArray<T> : PromotionLogicBase
@property (readonly) int32_t size __attribute__((swift_name("size")));
+ (instancetype)arrayWithSize:(int32_t)size init:(T _Nullable (^)(PromotionLogicInt *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (T _Nullable)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (id<PromotionLogicKotlinIterator>)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(T _Nullable)value __attribute__((swift_name("set(index:value:)")));
@end
__attribute__((swift_name("Kotlinx_serialization_coreSerializationStrategy")))
@protocol PromotionLogicKotlinx_serialization_coreSerializationStrategy
@required
- (void)serializeEncoder:(id<PromotionLogicKotlinx_serialization_coreEncoder>)encoder value:(id _Nullable)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<PromotionLogicKotlinx_serialization_coreSerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end
__attribute__((swift_name("Kotlinx_serialization_coreDeserializationStrategy")))
@protocol PromotionLogicKotlinx_serialization_coreDeserializationStrategy
@required
- (id _Nullable)deserializeDecoder:(id<PromotionLogicKotlinx_serialization_coreDecoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
@property (readonly) id<PromotionLogicKotlinx_serialization_coreSerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end
__attribute__((swift_name("Kotlinx_serialization_coreKSerializer")))
@protocol PromotionLogicKotlinx_serialization_coreKSerializer <PromotionLogicKotlinx_serialization_coreSerializationStrategy, PromotionLogicKotlinx_serialization_coreDeserializationStrategy>
@required
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/serialization/json/JsonElementSerializer))
*/
__attribute__((swift_name("Kotlinx_serialization_jsonJsonElement")))
@interface PromotionLogicKotlinx_serialization_jsonJsonElement : PromotionLogicBase
@property (class, readonly, getter=companion) PromotionLogicKotlinx_serialization_jsonJsonElementCompanion *companion __attribute__((swift_name("companion")));
@end
__attribute__((swift_name("Kotlinx_coroutines_coreCoroutineScope")))
@protocol PromotionLogicKotlinx_coroutines_coreCoroutineScope
@required
@property (readonly) id<PromotionLogicKotlinCoroutineContext> coroutineContext __attribute__((swift_name("coroutineContext")));
@end
__attribute__((swift_name("Kotlinx_serialization_coreSerialKind")))
@interface PromotionLogicKotlinx_serialization_coreSerialKind : PromotionLogicBase
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
__attribute__((swift_name("Kotlinx_serialization_corePolymorphicKind")))
@interface PromotionLogicKotlinx_serialization_corePolymorphicKind : PromotionLogicKotlinx_serialization_coreSerialKind
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_serialization_corePolymorphicKind.OPEN")))
@interface PromotionLogicKotlinx_serialization_corePolymorphicKindOPEN : PromotionLogicKotlinx_serialization_corePolymorphicKind
@property (class, readonly, getter=shared) PromotionLogicKotlinx_serialization_corePolymorphicKindOPEN *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)oPEN __attribute__((swift_name("init()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_serialization_corePolymorphicKind.SEALED")))
@interface PromotionLogicKotlinx_serialization_corePolymorphicKindSEALED : PromotionLogicKotlinx_serialization_corePolymorphicKind
@property (class, readonly, getter=shared) PromotionLogicKotlinx_serialization_corePolymorphicKindSEALED *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)sEALED __attribute__((swift_name("init()")));
@end
__attribute__((swift_name("Kotlinx_serialization_corePrimitiveKind")))
@interface PromotionLogicKotlinx_serialization_corePrimitiveKind : PromotionLogicKotlinx_serialization_coreSerialKind
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_serialization_corePrimitiveKind.BOOLEAN")))
@interface PromotionLogicKotlinx_serialization_corePrimitiveKindBOOLEAN : PromotionLogicKotlinx_serialization_corePrimitiveKind
@property (class, readonly, getter=shared) PromotionLogicKotlinx_serialization_corePrimitiveKindBOOLEAN *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)bOOLEAN __attribute__((swift_name("init()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_serialization_corePrimitiveKind.BYTE")))
@interface PromotionLogicKotlinx_serialization_corePrimitiveKindBYTE : PromotionLogicKotlinx_serialization_corePrimitiveKind
@property (class, readonly, getter=shared) PromotionLogicKotlinx_serialization_corePrimitiveKindBYTE *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)bYTE __attribute__((swift_name("init()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_serialization_corePrimitiveKind.CHAR")))
@interface PromotionLogicKotlinx_serialization_corePrimitiveKindCHAR : PromotionLogicKotlinx_serialization_corePrimitiveKind
@property (class, readonly, getter=shared) PromotionLogicKotlinx_serialization_corePrimitiveKindCHAR *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)cHAR __attribute__((swift_name("init()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_serialization_corePrimitiveKind.DOUBLE")))
@interface PromotionLogicKotlinx_serialization_corePrimitiveKindDOUBLE : PromotionLogicKotlinx_serialization_corePrimitiveKind
@property (class, readonly, getter=shared) PromotionLogicKotlinx_serialization_corePrimitiveKindDOUBLE *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)dOUBLE __attribute__((swift_name("init()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_serialization_corePrimitiveKind.FLOAT")))
@interface PromotionLogicKotlinx_serialization_corePrimitiveKindFLOAT : PromotionLogicKotlinx_serialization_corePrimitiveKind
@property (class, readonly, getter=shared) PromotionLogicKotlinx_serialization_corePrimitiveKindFLOAT *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)fLOAT __attribute__((swift_name("init()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_serialization_corePrimitiveKind.INT")))
@interface PromotionLogicKotlinx_serialization_corePrimitiveKindINT : PromotionLogicKotlinx_serialization_corePrimitiveKind
@property (class, readonly, getter=shared) PromotionLogicKotlinx_serialization_corePrimitiveKindINT *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)iNT __attribute__((swift_name("init()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_serialization_corePrimitiveKind.LONG")))
@interface PromotionLogicKotlinx_serialization_corePrimitiveKindLONG : PromotionLogicKotlinx_serialization_corePrimitiveKind
@property (class, readonly, getter=shared) PromotionLogicKotlinx_serialization_corePrimitiveKindLONG *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)lONG __attribute__((swift_name("init()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_serialization_corePrimitiveKind.SHORT")))
@interface PromotionLogicKotlinx_serialization_corePrimitiveKindSHORT : PromotionLogicKotlinx_serialization_corePrimitiveKind
@property (class, readonly, getter=shared) PromotionLogicKotlinx_serialization_corePrimitiveKindSHORT *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)sHORT __attribute__((swift_name("init()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_serialization_corePrimitiveKind.STRING")))
@interface PromotionLogicKotlinx_serialization_corePrimitiveKindSTRING : PromotionLogicKotlinx_serialization_corePrimitiveKind
@property (class, readonly, getter=shared) PromotionLogicKotlinx_serialization_corePrimitiveKindSTRING *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)sTRING __attribute__((swift_name("init()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_serialization_coreSerialKind.CONTEXTUAL")))
@interface PromotionLogicKotlinx_serialization_coreSerialKindCONTEXTUAL : PromotionLogicKotlinx_serialization_coreSerialKind
@property (class, readonly, getter=shared) PromotionLogicKotlinx_serialization_coreSerialKindCONTEXTUAL *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)cONTEXTUAL __attribute__((swift_name("init()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_serialization_coreSerialKind.ENUM")))
@interface PromotionLogicKotlinx_serialization_coreSerialKindENUM : PromotionLogicKotlinx_serialization_coreSerialKind
@property (class, readonly, getter=shared) PromotionLogicKotlinx_serialization_coreSerialKindENUM *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)eNUM __attribute__((swift_name("init()")));
@end
__attribute__((swift_name("Kotlinx_serialization_coreStructureKind")))
@interface PromotionLogicKotlinx_serialization_coreStructureKind : PromotionLogicKotlinx_serialization_coreSerialKind
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_serialization_coreStructureKind.CLASS")))
@interface PromotionLogicKotlinx_serialization_coreStructureKindCLASS : PromotionLogicKotlinx_serialization_coreStructureKind
@property (class, readonly, getter=shared) PromotionLogicKotlinx_serialization_coreStructureKindCLASS *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)cLASS __attribute__((swift_name("init()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_serialization_coreStructureKind.LIST")))
@interface PromotionLogicKotlinx_serialization_coreStructureKindLIST : PromotionLogicKotlinx_serialization_coreStructureKind
@property (class, readonly, getter=shared) PromotionLogicKotlinx_serialization_coreStructureKindLIST *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)lIST __attribute__((swift_name("init()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_serialization_coreStructureKind.MAP")))
@interface PromotionLogicKotlinx_serialization_coreStructureKindMAP : PromotionLogicKotlinx_serialization_coreStructureKind
@property (class, readonly, getter=shared) PromotionLogicKotlinx_serialization_coreStructureKindMAP *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)mAP __attribute__((swift_name("init()")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_serialization_coreStructureKind.OBJECT")))
@interface PromotionLogicKotlinx_serialization_coreStructureKindOBJECT : PromotionLogicKotlinx_serialization_coreStructureKind
@property (class, readonly, getter=shared) PromotionLogicKotlinx_serialization_coreStructureKindOBJECT *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)oBJECT __attribute__((swift_name("init()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/serialization/json/JsonPrimitiveSerializer))
*/
__attribute__((swift_name("Kotlinx_serialization_jsonJsonPrimitive")))
@interface PromotionLogicKotlinx_serialization_jsonJsonPrimitive : PromotionLogicKotlinx_serialization_jsonJsonElement
@property (class, readonly, getter=companion) PromotionLogicKotlinx_serialization_jsonJsonPrimitiveCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString *content __attribute__((swift_name("content")));
@property (readonly) BOOL isString __attribute__((swift_name("isString")));
- (NSString *)description __attribute__((swift_name("description()")));
@end
/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/serialization/json/JsonNullSerializer))
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_serialization_jsonJsonNull")))
@interface PromotionLogicKotlinx_serialization_jsonJsonNull : PromotionLogicKotlinx_serialization_jsonJsonPrimitive
@property (class, readonly, getter=shared) PromotionLogicKotlinx_serialization_jsonJsonNull *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *content __attribute__((swift_name("content")));
@property (readonly) BOOL isString __attribute__((swift_name("isString")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)jsonNull __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializerTypeParamsSerializers:(PromotionLogicKotlinArray<id<PromotionLogicKotlinx_serialization_coreKSerializer>> *)typeParamsSerializers __attribute__((swift_name("serializer(typeParamsSerializers:)")));
@end
__attribute__((swift_name("KotlinIterator")))
@protocol PromotionLogicKotlinIterator
@required
- (BOOL)hasNext __attribute__((swift_name("hasNext()")));
- (id _Nullable)next __attribute__((swift_name("next()")));
@end
__attribute__((swift_name("Kotlinx_serialization_coreEncoder")))
@protocol PromotionLogicKotlinx_serialization_coreEncoder
@required
- (id<PromotionLogicKotlinx_serialization_coreCompositeEncoder>)beginCollectionDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor collectionSize:(int32_t)collectionSize __attribute__((swift_name("beginCollection(descriptor:collectionSize:)")));
- (id<PromotionLogicKotlinx_serialization_coreCompositeEncoder>)beginStructureDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("beginStructure(descriptor:)")));
- (void)encodeBooleanValue:(BOOL)value __attribute__((swift_name("encodeBoolean(value:)")));
- (void)encodeByteValue:(int8_t)value __attribute__((swift_name("encodeByte(value:)")));
- (void)encodeCharValue:(unichar)value __attribute__((swift_name("encodeChar(value:)")));
- (void)encodeDoubleValue:(double)value __attribute__((swift_name("encodeDouble(value:)")));
- (void)encodeEnumEnumDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)enumDescriptor index:(int32_t)index __attribute__((swift_name("encodeEnum(enumDescriptor:index:)")));
- (void)encodeFloatValue:(float)value __attribute__((swift_name("encodeFloat(value:)")));
- (id<PromotionLogicKotlinx_serialization_coreEncoder>)encodeInlineDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("encodeInline(descriptor:)")));
- (void)encodeIntValue:(int32_t)value __attribute__((swift_name("encodeInt(value:)")));
- (void)encodeLongValue:(int64_t)value __attribute__((swift_name("encodeLong(value:)")));
/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNotNullMark __attribute__((swift_name("encodeNotNullMark()")));
/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNull __attribute__((swift_name("encodeNull()")));
/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNullableSerializableValueSerializer:(id<PromotionLogicKotlinx_serialization_coreSerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeNullableSerializableValue(serializer:value:)")));
- (void)encodeSerializableValueSerializer:(id<PromotionLogicKotlinx_serialization_coreSerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeSerializableValue(serializer:value:)")));
- (void)encodeShortValue:(int16_t)value __attribute__((swift_name("encodeShort(value:)")));
- (void)encodeStringValue:(NSString *)value __attribute__((swift_name("encodeString(value:)")));
@property (readonly) PromotionLogicKotlinx_serialization_coreSerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end
__attribute__((swift_name("Kotlinx_serialization_coreSerialDescriptor")))
@protocol PromotionLogicKotlinx_serialization_coreSerialDescriptor
@required
- (NSArray<id<PromotionLogicKotlinAnnotation>> *)getElementAnnotationsIndex:(int32_t)index __attribute__((swift_name("getElementAnnotations(index:)")));
- (id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)getElementDescriptorIndex:(int32_t)index __attribute__((swift_name("getElementDescriptor(index:)")));
- (int32_t)getElementIndexName:(NSString *)name __attribute__((swift_name("getElementIndex(name:)")));
- (NSString *)getElementNameIndex:(int32_t)index __attribute__((swift_name("getElementName(index:)")));
- (BOOL)isElementOptionalIndex:(int32_t)index __attribute__((swift_name("isElementOptional(index:)")));
@property (readonly) NSArray<id<PromotionLogicKotlinAnnotation>> *annotations __attribute__((swift_name("annotations")));
@property (readonly) int32_t elementsCount __attribute__((swift_name("elementsCount")));
@property (readonly) BOOL isInline __attribute__((swift_name("isInline")));
@property (readonly) BOOL isNullable __attribute__((swift_name("isNullable")));
@property (readonly) PromotionLogicKotlinx_serialization_coreSerialKind *kind __attribute__((swift_name("kind")));
@property (readonly) NSString *serialName __attribute__((swift_name("serialName")));
@end
__attribute__((swift_name("Kotlinx_serialization_coreDecoder")))
@protocol PromotionLogicKotlinx_serialization_coreDecoder
@required
- (id<PromotionLogicKotlinx_serialization_coreCompositeDecoder>)beginStructureDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("beginStructure(descriptor:)")));
- (BOOL)decodeBoolean __attribute__((swift_name("decodeBoolean()")));
- (int8_t)decodeByte __attribute__((swift_name("decodeByte()")));
- (unichar)decodeChar __attribute__((swift_name("decodeChar()")));
- (double)decodeDouble __attribute__((swift_name("decodeDouble()")));
- (int32_t)decodeEnumEnumDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)enumDescriptor __attribute__((swift_name("decodeEnum(enumDescriptor:)")));
- (float)decodeFloat __attribute__((swift_name("decodeFloat()")));
- (id<PromotionLogicKotlinx_serialization_coreDecoder>)decodeInlineDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("decodeInline(descriptor:)")));
- (int32_t)decodeInt __attribute__((swift_name("decodeInt()")));
- (int64_t)decodeLong __attribute__((swift_name("decodeLong()")));
/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)decodeNotNullMark __attribute__((swift_name("decodeNotNullMark()")));
/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (PromotionLogicKotlinNothing * _Nullable)decodeNull __attribute__((swift_name("decodeNull()")));
/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id _Nullable)decodeNullableSerializableValueDeserializer:(id<PromotionLogicKotlinx_serialization_coreDeserializationStrategy>)deserializer __attribute__((swift_name("decodeNullableSerializableValue(deserializer:)")));
- (id _Nullable)decodeSerializableValueDeserializer:(id<PromotionLogicKotlinx_serialization_coreDeserializationStrategy>)deserializer __attribute__((swift_name("decodeSerializableValue(deserializer:)")));
- (int16_t)decodeShort __attribute__((swift_name("decodeShort()")));
- (NSString *)decodeString __attribute__((swift_name("decodeString()")));
@property (readonly) PromotionLogicKotlinx_serialization_coreSerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_serialization_jsonJsonElement.Companion")))
@interface PromotionLogicKotlinx_serialization_jsonJsonElementCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicKotlinx_serialization_jsonJsonElementCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
__attribute__((swift_name("KotlinCoroutineContext")))
@protocol PromotionLogicKotlinCoroutineContext
@required
- (id _Nullable)foldInitial:(id _Nullable)initial operation:(id _Nullable (^)(id _Nullable, id<PromotionLogicKotlinCoroutineContextElement>))operation __attribute__((swift_name("fold(initial:operation:)")));
- (id<PromotionLogicKotlinCoroutineContextElement> _Nullable)getKey:(id<PromotionLogicKotlinCoroutineContextKey>)key __attribute__((swift_name("get(key:)")));
- (id<PromotionLogicKotlinCoroutineContext>)minusKeyKey:(id<PromotionLogicKotlinCoroutineContextKey>)key __attribute__((swift_name("minusKey(key:)")));
- (id<PromotionLogicKotlinCoroutineContext>)plusContext:(id<PromotionLogicKotlinCoroutineContext>)context __attribute__((swift_name("plus(context:)")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_serialization_jsonJsonPrimitive.Companion")))
@interface PromotionLogicKotlinx_serialization_jsonJsonPrimitiveCompanion : PromotionLogicBase
@property (class, readonly, getter=shared) PromotionLogicKotlinx_serialization_jsonJsonPrimitiveCompanion *shared __attribute__((swift_name("shared")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
- (id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end
__attribute__((swift_name("Kotlinx_serialization_coreCompositeEncoder")))
@protocol PromotionLogicKotlinx_serialization_coreCompositeEncoder
@required
- (void)encodeBooleanElementDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(BOOL)value __attribute__((swift_name("encodeBooleanElement(descriptor:index:value:)")));
- (void)encodeByteElementDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(int8_t)value __attribute__((swift_name("encodeByteElement(descriptor:index:value:)")));
- (void)encodeCharElementDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(unichar)value __attribute__((swift_name("encodeCharElement(descriptor:index:value:)")));
- (void)encodeDoubleElementDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(double)value __attribute__((swift_name("encodeDoubleElement(descriptor:index:value:)")));
- (void)encodeFloatElementDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(float)value __attribute__((swift_name("encodeFloatElement(descriptor:index:value:)")));
- (id<PromotionLogicKotlinx_serialization_coreEncoder>)encodeInlineElementDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("encodeInlineElement(descriptor:index:)")));
- (void)encodeIntElementDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(int32_t)value __attribute__((swift_name("encodeIntElement(descriptor:index:value:)")));
- (void)encodeLongElementDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(int64_t)value __attribute__((swift_name("encodeLongElement(descriptor:index:value:)")));
/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNullableSerializableElementDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index serializer:(id<PromotionLogicKotlinx_serialization_coreSerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeNullableSerializableElement(descriptor:index:serializer:value:)")));
- (void)encodeSerializableElementDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index serializer:(id<PromotionLogicKotlinx_serialization_coreSerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeSerializableElement(descriptor:index:serializer:value:)")));
- (void)encodeShortElementDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(int16_t)value __attribute__((swift_name("encodeShortElement(descriptor:index:value:)")));
- (void)encodeStringElementDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(NSString *)value __attribute__((swift_name("encodeStringElement(descriptor:index:value:)")));
- (void)endStructureDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("endStructure(descriptor:)")));
/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)shouldEncodeElementDefaultDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("shouldEncodeElementDefault(descriptor:index:)")));
@property (readonly) PromotionLogicKotlinx_serialization_coreSerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end
__attribute__((swift_name("Kotlinx_serialization_coreSerializersModule")))
@interface PromotionLogicKotlinx_serialization_coreSerializersModule : PromotionLogicBase
/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)dumpToCollector:(id<PromotionLogicKotlinx_serialization_coreSerializersModuleCollector>)collector __attribute__((swift_name("dumpTo(collector:)")));
/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<PromotionLogicKotlinx_serialization_coreKSerializer> _Nullable)getContextualKClass:(id<PromotionLogicKotlinKClass>)kClass typeArgumentsSerializers:(NSArray<id<PromotionLogicKotlinx_serialization_coreKSerializer>> *)typeArgumentsSerializers __attribute__((swift_name("getContextual(kClass:typeArgumentsSerializers:)")));
/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<PromotionLogicKotlinx_serialization_coreSerializationStrategy> _Nullable)getPolymorphicBaseClass:(id<PromotionLogicKotlinKClass>)baseClass value:(id)value __attribute__((swift_name("getPolymorphic(baseClass:value:)")));
/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<PromotionLogicKotlinx_serialization_coreDeserializationStrategy> _Nullable)getPolymorphicBaseClass:(id<PromotionLogicKotlinKClass>)baseClass serializedClassName:(NSString * _Nullable)serializedClassName __attribute__((swift_name("getPolymorphic(baseClass:serializedClassName:)")));
@end
__attribute__((swift_name("KotlinAnnotation")))
@protocol PromotionLogicKotlinAnnotation
@required
@end
__attribute__((swift_name("Kotlinx_serialization_coreCompositeDecoder")))
@protocol PromotionLogicKotlinx_serialization_coreCompositeDecoder
@required
- (BOOL)decodeBooleanElementDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeBooleanElement(descriptor:index:)")));
- (int8_t)decodeByteElementDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeByteElement(descriptor:index:)")));
- (unichar)decodeCharElementDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeCharElement(descriptor:index:)")));
- (int32_t)decodeCollectionSizeDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("decodeCollectionSize(descriptor:)")));
- (double)decodeDoubleElementDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeDoubleElement(descriptor:index:)")));
- (int32_t)decodeElementIndexDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("decodeElementIndex(descriptor:)")));
- (float)decodeFloatElementDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeFloatElement(descriptor:index:)")));
- (id<PromotionLogicKotlinx_serialization_coreDecoder>)decodeInlineElementDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeInlineElement(descriptor:index:)")));
- (int32_t)decodeIntElementDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeIntElement(descriptor:index:)")));
- (int64_t)decodeLongElementDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeLongElement(descriptor:index:)")));
/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id _Nullable)decodeNullableSerializableElementDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index deserializer:(id<PromotionLogicKotlinx_serialization_coreDeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeNullableSerializableElement(descriptor:index:deserializer:previousValue:)")));
/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)decodeSequentially __attribute__((swift_name("decodeSequentially()")));
- (id _Nullable)decodeSerializableElementDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index deserializer:(id<PromotionLogicKotlinx_serialization_coreDeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeSerializableElement(descriptor:index:deserializer:previousValue:)")));
- (int16_t)decodeShortElementDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeShortElement(descriptor:index:)")));
- (NSString *)decodeStringElementDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeStringElement(descriptor:index:)")));
- (void)endStructureDescriptor:(id<PromotionLogicKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("endStructure(descriptor:)")));
@property (readonly) PromotionLogicKotlinx_serialization_coreSerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinNothing")))
@interface PromotionLogicKotlinNothing : PromotionLogicBase
@end
__attribute__((swift_name("KotlinCoroutineContextElement")))
@protocol PromotionLogicKotlinCoroutineContextElement <PromotionLogicKotlinCoroutineContext>
@required
@property (readonly) id<PromotionLogicKotlinCoroutineContextKey> key __attribute__((swift_name("key")));
@end
__attribute__((swift_name("KotlinCoroutineContextKey")))
@protocol PromotionLogicKotlinCoroutineContextKey
@required
@end
/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
__attribute__((swift_name("Kotlinx_serialization_coreSerializersModuleCollector")))
@protocol PromotionLogicKotlinx_serialization_coreSerializersModuleCollector
@required
- (void)contextualKClass:(id<PromotionLogicKotlinKClass>)kClass provider:(id<PromotionLogicKotlinx_serialization_coreKSerializer> (^)(NSArray<id<PromotionLogicKotlinx_serialization_coreKSerializer>> *))provider __attribute__((swift_name("contextual(kClass:provider:)")));
- (void)contextualKClass:(id<PromotionLogicKotlinKClass>)kClass serializer:(id<PromotionLogicKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("contextual(kClass:serializer:)")));
- (void)polymorphicBaseClass:(id<PromotionLogicKotlinKClass>)baseClass actualClass:(id<PromotionLogicKotlinKClass>)actualClass actualSerializer:(id<PromotionLogicKotlinx_serialization_coreKSerializer>)actualSerializer __attribute__((swift_name("polymorphic(baseClass:actualClass:actualSerializer:)")));
- (void)polymorphicDefaultBaseClass:(id<PromotionLogicKotlinKClass>)baseClass defaultDeserializerProvider:(id<PromotionLogicKotlinx_serialization_coreDeserializationStrategy> _Nullable (^)(NSString * _Nullable))defaultDeserializerProvider __attribute__((swift_name("polymorphicDefault(baseClass:defaultDeserializerProvider:)"))) __attribute__((deprecated("Deprecated in favor of function with more precise name: polymorphicDefaultDeserializer")));
- (void)polymorphicDefaultDeserializerBaseClass:(id<PromotionLogicKotlinKClass>)baseClass defaultDeserializerProvider:(id<PromotionLogicKotlinx_serialization_coreDeserializationStrategy> _Nullable (^)(NSString * _Nullable))defaultDeserializerProvider __attribute__((swift_name("polymorphicDefaultDeserializer(baseClass:defaultDeserializerProvider:)")));
- (void)polymorphicDefaultSerializerBaseClass:(id<PromotionLogicKotlinKClass>)baseClass defaultSerializerProvider:(id<PromotionLogicKotlinx_serialization_coreSerializationStrategy> _Nullable (^)(id))defaultSerializerProvider __attribute__((swift_name("polymorphicDefaultSerializer(baseClass:defaultSerializerProvider:)")));
@end
__attribute__((swift_name("KotlinKDeclarationContainer")))
@protocol PromotionLogicKotlinKDeclarationContainer
@required
@end
__attribute__((swift_name("KotlinKAnnotatedElement")))
@protocol PromotionLogicKotlinKAnnotatedElement
@required
@end
/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
__attribute__((swift_name("KotlinKClassifier")))
@protocol PromotionLogicKotlinKClassifier
@required
@end
__attribute__((swift_name("KotlinKClass")))
@protocol PromotionLogicKotlinKClass <PromotionLogicKotlinKDeclarationContainer, PromotionLogicKotlinKAnnotatedElement, PromotionLogicKotlinKClassifier>
@required
/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
- (BOOL)isInstanceValue:(id _Nullable)value __attribute__((swift_name("isInstance(value:)")));
@property (readonly) NSString * _Nullable qualifiedName __attribute__((swift_name("qualifiedName")));
@property (readonly) NSString * _Nullable simpleName __attribute__((swift_name("simpleName")));
@end
#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
