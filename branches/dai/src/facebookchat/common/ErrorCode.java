package facebookchat.common;

public class ErrorCode {
	public static Long kError_Global_ValidationError = 1346001L;
	public static Long kError_Login_GenericError = 1348009L;
	public static Long kError_Chat_NotAvailable = 1356002L;
	public static Long kError_Chat_SendOtherNotAvailable = 1356003L;
	public static Long kError_Async_NotLoggedIn = 1357001L;
	public static Long kError_Async_LoginChanged = 1357003L;
	public static Long kError_Async_CSRFCheckFailed = 1357004L;
	public static Long kError_Chat_TooManyMessages = 1356008L;
	public static Long kError_Platform_CallbackValidationFailure = 1349007L;
	public static Long kError_Platform_ApplicationResponseInvalid = 1349008L;
	
	public static Long Error_Global_NoError = 0L;
	public static Long Error_Async_HttpConnectionFailed = 1001L;
	public static Long Error_Async_UnexpectedNullResponse = 1002L;
	public static Long Error_System_UIDNotFound = 1003L;
	public static Long Error_System_ChannelNotFound = 1004L;
	public static Long Error_System_PostFornIDNotFound = 1005L;
}