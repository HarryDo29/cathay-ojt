import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { User } from './common/entities/user.entity.js';
import { AuthModule } from './auth/auth.module.js';
import { UsersModule } from './users/users.module.js';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      envFilePath: '.env',
    }),
    TypeOrmModule.forRootAsync({
      inject: [ConfigService],
      useFactory: () => ({
        type: 'postgres',
        host: 'localhost',
        port: 5433,
        username: 'identity_service',
        password: 'generated_password_for_fun',
        database: 'postgres',
        entities: [User],
        synchronize: true,
      }),
    }),
    AuthModule,
    UsersModule,
  ],
})
export class AppModule {}
