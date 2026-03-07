import { MiddlewareConsumer, Module, NestModule } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Product } from './entities/product.entity.js';
import { Order } from './entities/order.entity.js';
import { OrderItem } from './entities/order-item.entity.js';
import { ProductsModule } from './products/products.module.js';
import { OrdersModule } from './orders/orders.module.js';
import { GatewayMiddleware } from './common/middlewares/gateway.middleware.js';
import { DataSeedService } from './common/seeding/data.seed.service.js';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      envFilePath: '.env',
    }),
    TypeOrmModule.forRootAsync({
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        type: 'postgres' as const,
        host: config.get<string>('DB_HOST'),
        port: config.get<number>('DB_PORT'),
        username: config.get<string>('DB_USERNAME'),
        password: config.get<string>('DB_PASSWORD'),
        database: config.get<string>('DB_NAME'),
        entities: [Product, Order, OrderItem],
        synchronize: true,
      }),
    }),
    TypeOrmModule.forFeature([Product, Order, OrderItem]),
    ProductsModule,
    OrdersModule,
  ],
  providers: [DataSeedService],
})
export class AppModule implements NestModule {
  configure(consumer: MiddlewareConsumer): void {
    consumer.apply(GatewayMiddleware).forRoutes('*');
  }
}
