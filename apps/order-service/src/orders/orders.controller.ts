import {
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  HttpStatus,
  Param,
  ParseUUIDPipe,
  Patch,
  Post,
  Request,
  UnauthorizedException,
} from '@nestjs/common';
import type { Request as ExpressRequest } from 'express';
import { OrdersService } from './orders.service.js';
import { CreateOrderDto } from './dto/create-order.dto.js';
import { UpdateOrderStatusDto } from './dto/update-order-status.dto.js';
import { Order } from 'src/entities/order.entity.js';

@Controller('orders')
export class OrdersController {
  constructor(private readonly ordersService: OrdersService) {}

  @Get('test')
  @HttpCode(HttpStatus.OK)
  async gatewayTest(@Request() req: ExpressRequest) {
    const ms = 2000;
    if (ms > 0) {
      await new Promise<void>((resolve) => setTimeout(resolve, ms));
    }
    return {
      ok: true,
      service: 'order-service',
      path: '/orders/test',
      method: req.method,
      timestamp: new Date().toISOString(),
      fromGateway: {
        'x-user-id': req.headers['x-user-id'] ?? null,
        'x-user-email': req.headers['x-user-email'] ?? null,
        'x-user-role': req.headers['x-user-role'] ?? null,
        'x-internal-api-key': req.headers['x-internal-api-key']
          ? '(set)'
          : null,
        'public-endpoint': req.headers['public-endpoint'] ?? null,
      },
    };
  }

  @Post()
  async create(
    @Request() req: ExpressRequest,
    @Body() dto: CreateOrderDto,
  ): Promise<Order | null> {
    const user_id = req.headers['x-user-id'] as string;
    if (!user_id) {
      throw new UnauthorizedException('Unauthorized');
    }
    return this.ordersService.create(user_id, dto);
  }

  @Get()
  async findAll() {
    return this.ordersService.findAll();
  }

  @Get(':id')
  async findOne(@Param('id', ParseUUIDPipe) id: string) {
    return this.ordersService.findById(id);
  }

  @Patch(':id/status')
  async updateStatus(
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: UpdateOrderStatusDto,
  ) {
    return this.ordersService.updateStatus(id, dto);
  }

  @Delete(':id')
  async cancel(@Param('id', ParseUUIDPipe) id: string) {
    return this.ordersService.cancel(id);
  }
}
