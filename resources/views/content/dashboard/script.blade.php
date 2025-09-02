<script>
    let trips = @json($trips);
    const labelColor = '#333';
    const borderColor = '#e4e6e8';

    let last7Days = [];
    for (let i = 6; i >= 0; i--) {
        let date = new Date();
        date.setDate(date.getDate() - i);
        last7Days.push(date.toISOString().split('T')[0]);
    }

    let tripCounts = last7Days.map(date => {
        return trips.filter(trip => trip.trip_date === date).length;
    });

    let categories = last7Days.map(date => {
        let formattedDate = new Date(date);
        return formattedDate.toLocaleDateString('en-US', {
            weekday: 'short',
            month: 'short',
            day: 'numeric'
        });
    });

    const horizontalBarChartEl = document.querySelector('#horizontalBarChart'),
        horizontalBarChartConfig = {
            chart: {
                height: 400,
                type: 'bar',
                toolbar: {
                    show: false
                }
            },
            plotOptions: {
                bar: {
                    horizontal: false,
                    columnWidth: '30%',
                    startingShape: 'rounded',
                    borderRadius: 8
                }
            },
            grid: {
                borderColor: borderColor,
                xaxis: {
                    lines: {
                        show: false
                    }
                },
                padding: {
                    top: -20,
                    bottom: -5
                }
            },
            colors: config.colors.primary,
            dataLabels: {
                enabled: false
            },
            series: [{
                data: tripCounts
            }],
            xaxis: {
                categories: categories,
                axisBorder: {
                    show: false
                },
                axisTicks: {
                    show: false
                },
                labels: {
                    style: {
                        colors: labelColor,
                        fontSize: '13px'
                    }
                }
            },
            yaxis: {
                labels: {
                    style: {
                        colors: labelColor,
                        fontSize: '13px'
                    }
                }
            }
        };

    if (horizontalBarChartEl !== undefined && horizontalBarChartEl !== null) {
        const horizontalBarChart = new ApexCharts(horizontalBarChartEl, horizontalBarChartConfig);
        horizontalBarChart.render();
    }
</script>
