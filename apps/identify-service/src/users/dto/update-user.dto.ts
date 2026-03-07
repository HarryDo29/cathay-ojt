import { IsEmail, IsOptional, IsString, MinLength } from 'class-validator';

/** DTO cập nhật thông tin user (email, username). Đổi mật khẩu dùng changePassword. */
export class UpdateUserDto {
  @IsOptional()
  @IsEmail()
  email?: string;

  @IsOptional()
  @IsString()
  @MinLength(2)
  username?: string;
}
