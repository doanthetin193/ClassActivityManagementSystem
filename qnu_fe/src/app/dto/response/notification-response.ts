export interface NotificationResponse {
    recipientId: number;
    message: string;
    classActivityId: number;
    notificationTime: string;
    notificationRecipientEnum: string;
    read: boolean;
}