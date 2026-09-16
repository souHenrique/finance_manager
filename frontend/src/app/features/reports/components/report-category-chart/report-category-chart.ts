import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export type ReportCategoryType = 'Receita' | 'Despesa';

export interface ReportChartEntry {
  label: string;
  amount: number;
  type: ReportCategoryType;
}

@Component({
  selector: 'app-report-category-chart',
  imports: [CurrencyPipe],
  templateUrl: './report-category-chart.html',
  styleUrl: './report-category-chart.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportCategoryChartComponent {
  readonly entries = input.required<ReportChartEntry[]>();
  readonly title = input.required<string>();
  readonly tableCaption = input.required<string>();

  readonly maximumAmount = computed(() =>
    Math.max(0, ...this.entries().map((entry) => entry.amount)),
  );

  barWidth(amount: number): number {
    const maximumAmount = this.maximumAmount();

    return maximumAmount === 0 ? 0 : (amount / maximumAmount) * 100;
  }

  chartDescription(): string {
    const entries = this.entries();

    if (entries.length === 0) {
      return `${this.title()}: não há dados por categoria.`;
    }

    return `${this.title()}: ${entries
      .map((entry) => `${entry.type} ${entry.label}: ${entry.amount.toFixed(2)}`)
      .join('; ')}.`;
  }
}
