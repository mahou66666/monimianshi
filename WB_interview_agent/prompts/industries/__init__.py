from .finance import FINANCE_PROMPT
from .healthcare import HEALTHCARE_PROMPT
from .manufacturing import MANUFACTURING_PROMPT
from .retail_ecommerce import RETAIL_ECOMMERCE_PROMPT

DEFAULT_PROMPT = "重点考察高并发、系统架构设计、数据库优化及代码规范。"

def get_industry_prompt(industry: str) -> str:
    """根据传入的 industry 字符串动态路由"""
    industry = str(industry or "")
    normalized = industry.lower()

    if any(keyword in industry for keyword in ["金融", "银行", "证券", "支付"]) or "finance" in normalized:
        return FINANCE_PROMPT
    if any(keyword in industry for keyword in ["医疗", "医院", "健康", "医药"]) or "health" in normalized:
        return HEALTHCARE_PROMPT
    if any(keyword in industry for keyword in ["制造", "工厂", "工业", "供应链"]) or "manufactur" in normalized:
        return MANUFACTURING_PROMPT
    if any(keyword in industry for keyword in ["零售", "电商", "商超", "消费"]) or "retail" in normalized or "ecommerce" in normalized:
        return RETAIL_ECOMMERCE_PROMPT

    # 默认兜底：互联网通用指南
    return DEFAULT_PROMPT
