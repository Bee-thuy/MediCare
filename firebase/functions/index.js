const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

async function notifyUser(userId, { title, body, type, appointmentId = "" }) {
  if (!userId) return;
  const notification = db.collection("notifications").doc();
  await notification.set({ userId, title, body, type, appointmentId, isRead: false, createdAt: admin.firestore.FieldValue.serverTimestamp() });
  const profile = await db.collection("users").doc(userId).get();
  const token = profile.get("fcmToken");
  if (!token) return;
  try {
    await admin.messaging().send({
      token,
      notification: { title, body },
      data: { type, appointmentId },
      android: {
        priority: "high",
        notification: {
          channelId: "medicare_updates",
          sound: "default",
        },
      },
    });
  } catch (error) {
    console.warn("Unable to send FCM", userId, error.code);
  }
}

exports.onAppointmentCreated = onDocumentCreated("appointments/{appointmentId}", async event => {
  const appointment = event.data.data();
  const admins = await db.collection("users").where("type", "==", "ADMIN").get();
  await Promise.all(admins.docs.map(adminProfile => notifyUser(adminProfile.id, {
    title: "Yêu cầu lịch khám mới",
    body: `${appointment.patientName || "Bệnh nhân"} đặt lịch với ${appointment.doctorName || "bác sĩ"}.`,
    type: "APPOINTMENT_CREATED",
    appointmentId: event.params.appointmentId,
  })));
});

exports.onAppointmentUpdated = onDocumentUpdated("appointments/{appointmentId}", async event => {
  const before = event.data.before.data();
  const after = event.data.after.data();
  const statusChanged = before.status !== after.status;
  const timeChanged = before.appointmentDate !== after.appointmentDate || before.startTime !== after.startTime;
  if (!statusChanged && !timeChanged) return;
  const statusText = { CONFIRMED: "đã được xác nhận", REJECTED: "đã bị từ chối", CANCELLED: "đã được hủy" }[after.status];
  await notifyUser(after.patientId, {
    title: timeChanged ? "Lịch khám đã được đổi" : "Cập nhật lịch khám",
    body: timeChanged ? `Lịch mới: ${after.appointmentDate} lúc ${after.startTime}.` : `Lịch khám của bạn ${statusText || "đã được cập nhật"}.`,
    type: timeChanged ? "APPOINTMENT_RESCHEDULED" : "APPOINTMENT_STATUS",
    appointmentId: event.params.appointmentId,
  });
});

exports.onLeaveRequestCreated = onDocumentCreated("doctorLeaveRequests/{requestId}", async event => {
  const request = event.data.data();
  const admins = await db.collection("users").where("type", "==", "ADMIN").get();
  await Promise.all(admins.docs.map(adminProfile => notifyUser(adminProfile.id, {
    title: "Yêu cầu nghỉ mới",
    body: `Bác sĩ gửi yêu cầu nghỉ ngày ${request.date}.`,
    type: "LEAVE_REQUEST",
  })));
});

exports.onLeaveRequestUpdated = onDocumentUpdated("doctorLeaveRequests/{requestId}", async event => {
  const before = event.data.before.data();
  const after = event.data.after.data();
  if (before.status === after.status || !["APPROVED", "REJECTED"].includes(after.status)) return;
  await notifyUser(after.doctorId, {
    title: "Kết quả yêu cầu nghỉ",
    body: `Yêu cầu nghỉ ngày ${after.date} đã được ${after.status === "APPROVED" ? "chấp thuận" : "từ chối"}.`,
    type: "LEAVE_DECISION",
  });
});

exports.onChatMessageCreated = onDocumentCreated("consultations/{appointmentId}/messages/{messageId}", async event => {
  const message = event.data.data();
  const appointment = await db.collection("appointments").doc(event.params.appointmentId).get();
  if (!appointment.exists) return;
  const data = appointment.data();
  const recipientId = message.senderId === data.patientId ? data.doctorId : data.patientId;
  await notifyUser(recipientId, {
    title: "Tin nhắn mới",
    body: message.content || "Bạn nhận được một tin nhắn mới.",
    type: "CHAT_MESSAGE",
    appointmentId: event.params.appointmentId,
  });
});
