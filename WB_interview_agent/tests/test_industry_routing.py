import unittest

from prompts.industries import get_industry_prompt


class IndustryRoutingTests(unittest.TestCase):
    def test_industry_prompt_finance_and_healthcare(self):
        finance_prompt = get_industry_prompt("金融科技后端")
        healthcare_prompt = get_industry_prompt("医疗信息化")
        self.assertIn("金融科技/银行风控", finance_prompt)
        self.assertIn("医疗健康行业", healthcare_prompt)

    def test_industry_prompt_default_fallback(self):
        default_prompt = get_industry_prompt("Java后端开发")
        self.assertIn("高并发", default_prompt)


if __name__ == "__main__":
    unittest.main()
