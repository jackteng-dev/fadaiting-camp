import { ElMessageBox } from "element-plus";

type ConfirmActionOptions = {
  title: string;
  message: string;
  confirmButtonText?: string;
  cancelButtonText?: string;
  type?: "success" | "warning" | "info" | "error";
};

export const confirmAdminAction = async ({
  title,
  message,
  confirmButtonText = "確定",
  cancelButtonText = "取消",
  type = "warning",
}: ConfirmActionOptions) => {
  try {
    await ElMessageBox.confirm(message, title, {
      confirmButtonText,
      cancelButtonText,
      type,
    });
    return true;
  } catch {
    return false;
  }
};

export const confirmSaveAction = (targetName: string, isEditing: boolean) =>
  confirmAdminAction({
    title: isEditing ? `確認修改${targetName}` : `確認新增${targetName}`,
    message: isEditing
      ? `確定要儲存這次的${targetName}修改嗎？`
      : `確定要新增這筆${targetName}嗎？`,
    confirmButtonText: isEditing ? "儲存修改" : "確認新增",
  });
